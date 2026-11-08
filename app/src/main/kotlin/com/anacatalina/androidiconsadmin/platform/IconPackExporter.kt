package com.anacatalina.androidiconsadmin.platform

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object IconPackExporter {

    data class ExportItem(
        val packageName: String,
        val appLabel: String,
        val iconDrawableName: String,
        val iconBitmap: Bitmap
    )

    fun exportPackToZip(
        context: Context,
        packName: String,
        items: List<ExportItem>,
        targetFile: File
    ) {
        val appFilterXml = buildAppFilterXml(items)
        val drawableXml = buildDrawableXml(items)
        val themeResourcesXml = buildThemeResourcesXml(items)

        ZipOutputStream(FileOutputStream(targetFile)).use { zip ->
            // 1. appfilter.xml
            zip.putNextEntry(ZipEntry("appfilter.xml"))
            zip.write(appFilterXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 2. assets/appfilter.xml (for legacy launcher compatibility)
            zip.putNextEntry(ZipEntry("assets/appfilter.xml"))
            zip.write(appFilterXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 3. drawable.xml
            zip.putNextEntry(ZipEntry("drawable.xml"))
            zip.write(drawableXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 4. theme_resources.xml (for Samsung / custom theme engines)
            zip.putNextEntry(ZipEntry("theme_resources.xml"))
            zip.write(themeResourcesXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 5. Individual icons as PNGs
            for (item in items) {
                zip.putNextEntry(ZipEntry("icons/${item.iconDrawableName}.png"))
                val baos = ByteArrayOutputStream()
                item.iconBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                zip.write(baos.toByteArray())
                zip.closeEntry()
            }
        }
    }

    fun exportToGalleryForOneUiThemePark(
        context: Context,
        albumName: String,
        items: List<ExportItem>
    ): Int {
        var exportedCount = 0
        val resolver = context.contentResolver

        for (item in items) {
            val fileName = "${item.iconDrawableName}.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/AndroidIconsAdmin/$albumName"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                try {
                    resolver.openOutputStream(uri)?.use { os ->
                        item.iconBitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                    exportedCount++
                } catch (_: Exception) {
                    resolver.delete(uri, null, null)
                }
            }
        }
        return exportedCount
    }

    private fun buildAppFilterXml(items: List<ExportItem>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        sb.append("<resources>\n")
        sb.append("    <iconback img=\"iconback\" />\n")
        sb.append("    <iconmask img=\"iconmask\" />\n")
        sb.append("    <scale factor=\"0.86\" />\n")
        for (item in items) {
            sb.append("    <item component=\"ComponentInfo{${item.packageName}/${item.packageName}.MainActivity}\" drawable=\"${item.iconDrawableName}\" />\n")
        }
        sb.append("</resources>\n")
        return sb.toString()
    }

    private fun buildDrawableXml(items: List<ExportItem>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        sb.append("<resources>\n")
        sb.append("    <category title=\"Custom Icons\">\n")
        for (item in items) {
            sb.append("        <item drawable=\"${item.iconDrawableName}\" />\n")
        }
        sb.append("    </category>\n")
        sb.append("</resources>\n")
        return sb.toString()
    }

    private fun buildThemeResourcesXml(items: List<ExportItem>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        sb.append("<theme>\n")
        for (item in items) {
            sb.append("    <AppIcon name=\"${item.packageName}\" image=\"${item.iconDrawableName}\" />\n")
        }
        sb.append("</theme>\n")
        return sb.toString()
    }
}
