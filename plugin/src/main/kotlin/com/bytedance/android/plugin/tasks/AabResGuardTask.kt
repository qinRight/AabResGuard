package com.bytedance.android.plugin.tasks

import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.scope.VariantScope
import com.bytedance.android.aabresguard.commands.ObfuscateBundleCommand
import com.bytedance.android.plugin.extensions.AabResGuardExtension
import com.bytedance.android.plugin.internal.getBundleFilePath
import com.bytedance.android.plugin.internal.getSigningConfig
import com.bytedance.android.plugin.internal.invokeSigningConfig
import com.bytedance.android.plugin.model.SigningConfig
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Path

/**
 * Created by YangJing on 2019/10/15 .
 * Email: yangjing.yeoh@bytedance.com
 */
open class AabResGuardTask : DefaultTask() {

    private lateinit var variantScope: VariantScope
    lateinit var signingConfig: SigningConfig
    var aabResGuard: AabResGuardExtension = project.extensions.getByName("aabResGuard") as AabResGuardExtension
    private lateinit var bundlePath: Path
    private lateinit var obfuscatedBundlePath: Path

    init {
        description = "Assemble resource proguard for bundle file"
        group = "bundle"
        outputs.upToDateWhen { false }
    }

    fun setVariantScope(variantScope: VariantScope) {
        this.variantScope = variantScope
        // init bundleFile, obfuscatedBundlePath must init before task action.
        bundlePath = getBundleFilePath(project, variantScope)
        obfuscatedBundlePath = File(bundlePath.toFile().parentFile, aabResGuard.obfuscatedBundleFileName).toPath()
    }

    fun getObfuscatedBundlePath(): Path {
        return obfuscatedBundlePath
    }

    @TaskAction
    private fun execute() {
        println(aabResGuard.toString())
        // init signing config
//        signingConfig = getSigningConfig(project, variantScope)
        val android = project.extensions.getByType(AppExtension::class.java)

        signingConfig = invokeSigningConfig(android.signingConfigs.getByName("release"))//getSigningConfig(project, variantScope)

        printSignConfiguration()

        prepareUnusedFile()

        var countnum = 1
        try {
            println("sign ObfuscateBundleCommand create ${countnum++}")
            val command = ObfuscateBundleCommand.builder()
            println("sign ObfuscateBundleCommand create ${countnum++}")
            command.setEnableObfuscate(aabResGuard.enableObfuscate)
            println("sign ObfuscateBundleCommand create ${countnum++}")
            command.setBundlePath(bundlePath)
            println("sign ObfuscateBundleCommand create ${countnum++}")
            command.setOutputPath(obfuscatedBundlePath)
            println("sign ObfuscateBundleCommand create ${countnum++}")
            command.setMergeDuplicatedResources(aabResGuard.mergeDuplicatedRes)
            println("sign ObfuscateBundleCommand create ${countnum++}")
            command.setWhiteList(aabResGuard.whiteList)
            println("sign ObfuscateBundleCommand create ${countnum++}")
            command.setFilterFile(aabResGuard.enableFilterFiles)
            println("sign ObfuscateBundleCommand create ${countnum++}")
            command.setFileFilterRules(aabResGuard.filterList)
            println("sign ObfuscateBundleCommand create ${countnum++}")
            command.setRemoveStr(aabResGuard.enableFilterStrings)
            println("sign ObfuscateBundleCommand create ${countnum++}")
            command.setUnusedStrPath(aabResGuard.unusedStringPath)
            println("sign ObfuscateBundleCommand create ${countnum++}")
            command.setLanguageWhiteList(aabResGuard.languageWhiteList)
            println("sign ObfuscateBundleCommand create ${countnum}")
            if (aabResGuard.mappingFile != null) {
                command.setMappingPath(aabResGuard.mappingFile)
                println("sign ObfuscateBundleCommand setMappingPath")
            }

            if (signingConfig.storeFile != null && signingConfig.storeFile!!.exists()) {
                command.setStoreFile(signingConfig.storeFile!!.toPath())
                command.setKeyAlias(signingConfig.keyAlias)
                command.setKeyPassword(signingConfig.keyPassword)
                command.setStorePassword(signingConfig.storePassword)
                println("sign ObfuscateBundleCommand setStoreFile")
            }
            command.build().execute()
            println("sign ObfuscateBundleCommand execute")
        }catch (e:Throwable){
            e.printStackTrace()
            println("sign ObfuscateBundleCommand ${e.toString()}")
        }
    }

    private fun prepareUnusedFile() {
        val simpleName = variantScope.variantData.name.replace("Release", "")
        val name = simpleName[0].toLowerCase() + simpleName.substring(1)
        val resourcePath = "${project.buildDir}/outputs/mapping/$name/release/unused.txt"
        val usedFile = File(resourcePath)
        if (usedFile.exists()) {
            println("find unused.txt : ${usedFile.absolutePath}")
            if (aabResGuard.enableFilterStrings) {
                if (aabResGuard.unusedStringPath == null || aabResGuard.unusedStringPath!!.isBlank()) {
                    aabResGuard.unusedStringPath = usedFile.absolutePath
                    println("replace unused.txt!")
                }
            }
        } else {
            println("not exists unused.txt : ${usedFile.absolutePath}\n" +
                    "use default path : ${aabResGuard.unusedStringPath}")
        }
    }

    private fun printSignConfiguration() {
        println("-------------- sign configuration --------------")
        println("\tstoreFile : ${signingConfig.storeFile}")
        println("\tkeyPassword : ${encrypt(signingConfig.keyPassword)}")
        println("\talias : ${encrypt(signingConfig.keyAlias)}")
        println("\tstorePassword : ${encrypt(signingConfig.storePassword)}")
        println("-------------- sign configuration --------------")
    }

    private fun encrypt(value: String?): String {
        if (value == null) return "/"
        if (value.length > 2) {
            return "${value.substring(0, value.length / 2)}****"
        }
        return "****"
    }
}