package gg.ginco.jellyparty.codec.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.toTypeName
import gg.ginco.jellyparty.codec.annotations.SerializableAsset
import gg.ginco.jellyparty.codec.annotations.SerializableObject
import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerialIgnore
import gg.ginco.jellyparty.codec.annotations.SerialWithCodec

data class AssetRegistration(val packageName: String, val className: String, val path: String)

data class FieldCodecInfo(
    val codecExpr: String,
    val setter: String,
    val getter: String,
    val imports: List<String> = emptyList()
)

class CodecProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val unableToProcess = mutableListOf<KSAnnotated>()
        val registrations = mutableListOf<AssetRegistration>()
        val sourceFiles = mutableListOf<KSFile>()

        resolver.getSymbolsWithAnnotation(SerializableAsset::class.qualifiedName!!).forEach { symbol ->
            if (symbol.validate() && symbol is KSClassDeclaration) {
                symbol.accept(CodecVisitor(codeGenerator, logger, isAsset = true), Unit)

                val path = symbol.annotations
                    .first { it.shortName.asString() == "SerializableAsset" }
                    .arguments.firstOrNull { it.name?.asString() == "path" }
                    ?.value as? String ?: ""

                if (path.isNotEmpty()) {
                    registrations.add(AssetRegistration(symbol.packageName.asString(), symbol.simpleName.asString(), path))
                    symbol.containingFile?.let { sourceFiles.add(it) }
                }
            } else if (!symbol.validate()) {
                unableToProcess.add(symbol)
            }
        }

        resolver.getSymbolsWithAnnotation(SerializableObject::class.qualifiedName!!).forEach { symbol ->
            if (symbol.validate() && symbol is KSClassDeclaration) {
                symbol.accept(CodecVisitor(codeGenerator, logger, isAsset = false), Unit)
            } else if (!symbol.validate()) {
                unableToProcess.add(symbol)
            }
        }

        if (unableToProcess.isEmpty() && registrations.isNotEmpty()) {
            generateRegistrationFile(registrations, sourceFiles)
        }

        return unableToProcess
    }

    private fun generateRegistrationFile(registrations: List<AssetRegistration>, sourceFiles: List<KSFile>) {
        val packageName = registrations.first().packageName
        val content = buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import com.hypixel.hytale.assetstore.AssetRegistry")
            appendLine("import com.hypixel.hytale.assetstore.map.DefaultAssetMap")
            appendLine("import com.hypixel.hytale.server.core.asset.HytaleAssetStore")
            appendLine()
            appendLine("fun registerAssets() {")
            registrations.forEach { (_, className, path) ->
                appendLine("    AssetRegistry.register(")
                appendLine("        HytaleAssetStore.builder(${className}::class.java, DefaultAssetMap())")
                appendLine("            .setPath(\"$path\")")
                appendLine("            .setCodec(${className}Codec)")
                appendLine("            .setKeyFunction(${className}::getId)")
                appendLine("            .build()")
                appendLine("    )")
            }
            appendLine("}")
        }

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(true, *sourceFiles.toTypedArray()),
            packageName = packageName,
            fileName = "AssetRegistration"
        )
        file.bufferedWriter().use { it.write(content) }
    }
}

class CodecVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val isAsset: Boolean
) : KSVisitorVoid() {

    companion object {
        private val PROTOCOL_IMPORTS = listOf(
            "com.hypixel.hytale.protocol.*",
            "com.hypixel.hytale.server.core.codec.ProtocolCodecs",
            "com.hypixel.hytale.math.vector.Vector2fUtil",
            "com.hypixel.hytale.math.vector.Vector3fUtil",
            "com.hypixel.hytale.math.vector.Vector3dUtil",
            "com.hypixel.hytale.math.vector.Vector3iUtil",
            "org.joml.Vector2f",
            "org.joml.Vector3f",
            "org.joml.Vector3d",
            "org.joml.Vector3i"
        )
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val className = classDeclaration.simpleName.asString()
        val packageName = classDeclaration.packageName.asString()

        val properties = classDeclaration.getAllProperties()
            .filter {
                it.isMutable &&
                !it.annotations.any { a -> a.shortName.asString() == "SerialIgnore" } &&
                it.simpleName.asString() != "_id" &&
                it.simpleName.asString() != "_data"
            }
            .toList()

        val annotationName = if (isAsset) "SerializableAsset" else "SerializableObject"
        val extraImports = classDeclaration.annotations
            .first { it.shortName.asString() == annotationName }
            .arguments.firstOrNull { it.name?.asString() == "extraImports" }
            ?.value.let { it as? List<*> ?: emptyList<Any>() }
            .filterIsInstance<String>()

        val content = if (isAsset) {
            buildAssetFileContent(packageName, className, properties, extraImports)
        } else {
            buildObjectFileContent(packageName, className, properties, extraImports)
        }

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false, classDeclaration.containingFile!!),
            packageName = packageName,
            fileName = "${className}Codec"
        )

        file.bufferedWriter().use { it.write(content) }
    }

    private fun buildAssetFileContent(
        packageName: String,
        className: String,
        properties: List<KSPropertyDeclaration>,
        extraImports: List<String> = emptyList()
    ): String = buildString {
        val fieldInfos = properties.map { it to determineFieldCodecInfo(it) }
        val allImports = (fieldInfos.flatMap { (_, info) -> info.imports } + extraImports)
            .distinct().sorted()

        appendLine("package $packageName")
        appendLine()
        appendLine("import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec")
        appendLine("import com.hypixel.hytale.codec.Codec")
        appendLine("import com.hypixel.hytale.codec.KeyedCodec")
        allImports.forEach { appendLine("import $it") }
        appendLine("import kotlin.jvm.JvmField")
        appendLine()
        appendLine("@JvmField")
        append("public val ${className}Codec = AssetBuilderCodec.builder(")
        appendLine()
        appendLine("    ${className}::class.java,")
        appendLine("    { ${className}() },")
        appendLine("    Codec.STRING,")
        appendLine("    { asset, key -> asset._id = key },")
        appendLine("    { asset -> asset._id },")
        appendLine("    { asset, data -> asset._data = data },")
        appendLine("    { asset -> asset._data }")
        append(")")

        appendFields(fieldInfos)

        appendLine()
        appendLine(".build()")
    }

    private fun buildObjectFileContent(
        packageName: String,
        className: String,
        properties: List<KSPropertyDeclaration>,
        extraImports: List<String> = emptyList()
    ): String = buildString {
        val fieldInfos = properties.map { it to determineFieldCodecInfo(it) }
        val allImports = (fieldInfos.flatMap { (_, info) -> info.imports } + extraImports)
            .distinct().sorted()

        appendLine("package $packageName")
        appendLine()
        appendLine("import com.hypixel.hytale.codec.Codec")
        appendLine("import com.hypixel.hytale.codec.KeyedCodec")
        appendLine("import com.hypixel.hytale.codec.builder.BuilderCodec")
        allImports.forEach { appendLine("import $it") }
        appendLine("import kotlin.jvm.JvmField")
        appendLine()
        appendLine("@JvmField")
        append("public val ${className}Codec = BuilderCodec.builder(")
        appendLine()
        appendLine("    ${className}::class.java,")
        appendLine("    { ${className}() }")
        append(")")

        appendFields(fieldInfos)

        appendLine()
        appendLine(".build()")
    }

    private fun StringBuilder.appendFields(fieldInfos: List<Pair<KSPropertyDeclaration, FieldCodecInfo>>) {
        fieldInfos.forEach { (property, info) ->
            val serialName = property.annotations
                .firstOrNull { it.shortName.asString() == "SerialName" }
                ?.arguments?.firstOrNull { it.name?.asString() == "value" }
                ?.value as? String ?: property.simpleName.asString().replaceFirstChar { it.uppercase() }

            appendLine()
            appendLine(".append(")
            appendLine("    KeyedCodec(\"$serialName\", ${info.codecExpr}),")
            appendLine("    ${info.setter},")
            appendLine("    ${info.getter}")
            append(")")
            appendLine()
            append(".add()")
        }
    }

    private fun determineFieldCodecInfo(property: KSPropertyDeclaration): FieldCodecInfo {
        val propName = property.simpleName.asString()
        val typeName = property.type.resolve()
        val qualifiedName = typeName.declaration.qualifiedName?.asString()

        val customCodec = property.annotations
            .firstOrNull { it.shortName.asString() == "SerialWithCodec" }
            ?.arguments?.firstOrNull { it.name?.asString() == "codecClass" }
            ?.value as? String

        if (customCodec != null) {
            return FieldCodecInfo(customCodec, defaultSetter(propName), defaultGetter(propName))
        }

        return when (qualifiedName) {
            "kotlin.String"  -> simple("Codec.STRING", propName)
            "kotlin.Int"     -> simple("Codec.INTEGER", propName)
            "kotlin.Float"   -> simple("Codec.FLOAT", propName)
            "kotlin.Double"  -> simple("Codec.DOUBLE", propName)
            "kotlin.Boolean" -> simple("Codec.BOOLEAN", propName)
            "kotlin.IntArray" -> simple("Codec.INT_ARRAY", propName)
            "java.util.UUID" -> simple("Codec.UUID_STRING", propName)
            "kotlin.collections.List", "kotlin.collections.MutableList" -> {
                val el = elementType(typeName)
                when {
                    el == "kotlin.String" ->
                        listField("Codec.STRING_ARRAY", propName, imports = emptyList())
                    el == "kotlin.Int" ->
                        listField("Codec.INT_ARRAY", propName, imports = emptyList(), toArray = "toIntArray")
                    el == "kotlin.Double" ->
                        listField("Codec.DOUBLE_ARRAY", propName, imports = emptyList(), toArray = "toDoubleArray")
                    el == "kotlin.Float" ->
                        listField("Codec.FLOAT_ARRAY", propName, imports = emptyList(), toArray = "toFloatArray")
                    isSerializableElement(elementTypeDeclaration(typeName)) ->
                        listField("ArrayCodec(${el}Codec) { size -> arrayOfNulls<$el>(size) }", propName)
                    el == "java.util.UUID" ->
                        listField("ArrayCodec(Codec.UUID_STRING) { size -> arrayOfNulls<java.util.UUID>(size) }", propName)
                    else -> {
                        // Silent STRING_ARRAY degradation corrupts object and
                        // numeric lists at RUNTIME (decode throws or drops
                        // data) — a build-time error is the honest failure.
                        logger.error(
                            "Property '$propName': List<$el> is not supported. " +
                            "Supported list elements: String, UUID, Int, Double, Float, " +
                            "and @SerializableObject/@SerializableAsset classes.",
                            property
                        )
                        listField("Codec.STRING_ARRAY", propName, imports = emptyList())
                    }
                }
            }
            "kotlin.Array" -> {
                val el = elementType(typeName)
                val codec = when (el) {
                    "kotlin.Int"    -> "Codec.INT_ARRAY"
                    "kotlin.String" -> "Codec.STRING_ARRAY"
                    else            -> "Codec.STRING_ARRAY"
                }
                simple(codec, propName)
            }
            "com.hypixel.hytale.protocol.Direction" ->
                FieldCodecInfo("ProtocolCodecs.DIRECTION", defaultSetter(propName), defaultGetter(propName), PROTOCOL_IMPORTS)
            // JOML vector types (0.5+)
            "org.joml.Vector3f" ->
                FieldCodecInfo("Vector3fUtil.CODEC", defaultSetter(propName), defaultGetter(propName), PROTOCOL_IMPORTS)
            "org.joml.Vector2f" ->
                FieldCodecInfo("Vector2fUtil.CODEC", defaultSetter(propName), defaultGetter(propName), PROTOCOL_IMPORTS)
            "org.joml.Vector3d" ->
                FieldCodecInfo("Vector3dUtil.CODEC", defaultSetter(propName), defaultGetter(propName), PROTOCOL_IMPORTS)
            "org.joml.Vector3i" ->
                FieldCodecInfo("Vector3iUtil.CODEC", defaultSetter(propName), defaultGetter(propName), PROTOCOL_IMPORTS)
            "com.hypixel.hytale.protocol.Position" ->
                FieldCodecInfo(
                    "Vector3fUtil.CODEC",
                    "{ obj, value -> obj.$propName = Position(value.x.toDouble(), value.y.toDouble(), value.z.toDouble()) }",
                    "{ obj -> obj.$propName?.let { Vector3f(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) } }",
                    PROTOCOL_IMPORTS
                )
            else -> {
                val decl = typeName.declaration
                if (decl is KSClassDeclaration && decl.classKind == ClassKind.ENUM_CLASS) {
                    FieldCodecInfo(
                        "EnumCodec(${typeName.makeNotNullable().toTypeName()}::class.java)",
                        defaultSetter(propName),
                        defaultGetter(propName),
                        listOf("com.hypixel.hytale.codec.codecs.EnumCodec")
                    )
                } else {
                    simple("Codec.STRING", propName)
                }
            }
        }
    }

    private fun simple(codec: String, propName: String) =
        FieldCodecInfo(codec, defaultSetter(propName), defaultGetter(propName))

    private fun listField(
        codec: String,
        propName: String,
        imports: List<String> = listOf("com.hypixel.hytale.codec.codecs.array.ArrayCodec"),
        toArray: String = "toTypedArray"
    ) = FieldCodecInfo(
        codec,
        "{ obj, value -> obj.$propName = value.toMutableList() }",
        "{ obj -> obj.$propName.$toArray() }",
        imports
    )

    private fun defaultSetter(propName: String) = "{ obj, value -> obj.$propName = value }"
    private fun defaultGetter(propName: String) = "{ obj -> obj.$propName }"

    private fun elementType(typeName: KSType) =
        typeName.arguments.firstOrNull()?.type?.resolve()?.declaration?.qualifiedName?.asString()

    private fun elementTypeDeclaration(typeName: KSType): KSDeclaration? =
        typeName.arguments.firstOrNull()?.type?.resolve()?.declaration

    private fun isSerializableElement(declaration: KSDeclaration?): Boolean =
        declaration is KSClassDeclaration && declaration.annotations.any {
            it.shortName.asString() == "SerializableObject" || it.shortName.asString() == "SerializableAsset"
        }
}

class CodecProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return CodecProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options
        )
    }
}
