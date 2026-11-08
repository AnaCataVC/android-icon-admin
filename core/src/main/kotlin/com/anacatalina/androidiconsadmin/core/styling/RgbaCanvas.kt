package com.anacatalina.androidiconsadmin.core.styling

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import kotlin.math.abs

class RgbaCanvas(
    val width: Int,
    val height: Int,
    val pixels: ByteArray = ByteArray(width * height * 4)
) {
    init {
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }
        require(pixels.size == width * height * 4) { "Pixel array size must match width * height * 4" }
    }

    data class Pixel(val r: Int, val g: Int, val b: Int, val a: Int)

    fun getPixel(x: Int, y: Int): Pixel {
        require(x in 0 until width && y in 0 until height) { "Coordinates out of bounds: ($x, $y)" }
        val offset = (y * width + x) * 4
        return Pixel(
            r = pixels[offset].toInt() and 0xFF,
            g = pixels[offset + 1].toInt() and 0xFF,
            b = pixels[offset + 2].toInt() and 0xFF,
            a = pixels[offset + 3].toInt() and 0xFF
        )
    }

    fun setPixel(x: Int, y: Int, r: Int, g: Int, b: Int, a: Int) {
        if (x !in 0 until width || y !in 0 until height) return
        val offset = (y * width + x) * 4
        pixels[offset] = r.coerceIn(0, 255).toByte()
        pixels[offset + 1] = g.coerceIn(0, 255).toByte()
        pixels[offset + 2] = b.coerceIn(0, 255).toByte()
        pixels[offset + 3] = a.coerceIn(0, 255).toByte()
    }

    fun clone(): RgbaCanvas {
        val copy = ByteArray(pixels.size)
        System.arraycopy(pixels, 0, copy, 0, pixels.size)
        return RgbaCanvas(width, height, copy)
    }

    fun toPngByteArray(): ByteArray {
        val out = ByteArrayOutputStream()
        // Standard PNG Signature
        out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))

        // IHDR Chunk
        val ihdr = ByteArrayOutputStream()
        val ihdrDos = DataOutputStream(ihdr)
        ihdrDos.writeInt(width)
        ihdrDos.writeInt(height)
        ihdrDos.writeByte(8) // bit depth: 8 bits per channel
        ihdrDos.writeByte(6) // color type: 6 (RGBA)
        ihdrDos.writeByte(0) // compression: 0 (deflate)
        ihdrDos.writeByte(0) // filter method: 0
        ihdrDos.writeByte(0) // interlace: 0 (none)
        writeChunk(out, "IHDR", ihdr.toByteArray())

        // IDAT Chunk
        val bpp = 4
        val raw = ByteArray(height * (1 + width * bpp))
        for (y in 0 until height) {
            val rawOffset = y * (1 + width * bpp)
            raw[rawOffset] = 0 // Filter None
            System.arraycopy(pixels, y * width * bpp, raw, rawOffset + 1, width * bpp)
        }
        val idat = ByteArrayOutputStream()
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION)
        val dos = DeflaterOutputStream(idat, deflater)
        dos.write(raw)
        dos.finish()
        writeChunk(out, "IDAT", idat.toByteArray())

        // IEND Chunk
        writeChunk(out, "IEND", ByteArray(0))
        return out.toByteArray()
    }

    companion object {
        private fun writeChunk(out: OutputStream, type: String, data: ByteArray) {
            val dos = DataOutputStream(out)
            dos.writeInt(data.size)
            val typeBytes = type.toByteArray(Charsets.US_ASCII)
            dos.write(typeBytes)
            dos.write(data)
            val crc = CRC32()
            crc.update(typeBytes)
            crc.update(data)
            dos.writeInt(crc.value.toInt())
        }

        fun fromPngByteArray(bytes: ByteArray): RgbaCanvas {
            require(bytes.size >= 8) { "Invalid PNG: data too short" }
            val inStream = DataInputStream(ByteArrayInputStream(bytes))
            val sig = ByteArray(8)
            inStream.readFully(sig)
            if (sig[0] != 0x89.toByte() || sig[1] != 0x50.toByte() || sig[2] != 0x4E.toByte() || sig[3] != 0x47.toByte() ||
                sig[4] != 0x0D.toByte() || sig[5] != 0x0A.toByte() || sig[6] != 0x1A.toByte() || sig[7] != 0x0A.toByte()
            ) {
                throw IllegalArgumentException("Invalid PNG signature")
            }

            var width = 0
            var height = 0
            var bpp = 4
            var colorType = 6
            val idatOut = ByteArrayOutputStream()

            while (inStream.available() > 0) {
                val len = inStream.readInt()
                val typeBytes = ByteArray(4)
                inStream.readFully(typeBytes)
                val type = String(typeBytes, Charsets.US_ASCII)
                val data = ByteArray(len)
                inStream.readFully(data)
                inStream.readInt() // CRC

                when (type) {
                    "IHDR" -> {
                        val ihdrIn = DataInputStream(ByteArrayInputStream(data))
                        width = ihdrIn.readInt()
                        height = ihdrIn.readInt()
                        val bitDepth = ihdrIn.readUnsignedByte()
                        colorType = ihdrIn.readUnsignedByte()
                        bpp = when (colorType) {
                            6 -> 4 // RGBA
                            2 -> 3 // RGB
                            4 -> 2 // Grayscale + Alpha
                            0 -> 1 // Grayscale
                            else -> 4
                        }
                    }
                    "IDAT" -> {
                        idatOut.write(data)
                    }
                    "IEND" -> {
                        break
                    }
                }
            }

            if (width <= 0 || height <= 0) {
                throw IllegalArgumentException("Invalid PNG dimensions: ${width}x${height}")
            }

            val decompressedStream = InflaterInputStream(ByteArrayInputStream(idatOut.toByteArray()), Inflater())
            val decompressedBytes = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var n: Int
            while (decompressedStream.read(buf).also { n = it } != -1) {
                decompressedBytes.write(buf, 0, n)
            }
            val raw = decompressedBytes.toByteArray()

            val canvas = RgbaCanvas(width, height)
            val rowBytes = width * bpp
            val stride = 1 + rowBytes
            var prevRow = ByteArray(rowBytes)

            for (y in 0 until height) {
                val rawOffset = y * stride
                if (rawOffset >= raw.size) break
                val filter = raw[rawOffset].toInt() and 0xFF
                val curRow = ByteArray(rowBytes)

                for (x in 0 until rowBytes) {
                    val filt = if (rawOffset + 1 + x < raw.size) raw[rawOffset + 1 + x].toInt() and 0xFF else 0
                    val a = if (x >= bpp) curRow[x - bpp].toInt() and 0xFF else 0
                    val b = prevRow[x].toInt() and 0xFF
                    val c = if (x >= bpp) prevRow[x - bpp].toInt() and 0xFF else 0

                    val recon = when (filter) {
                        0 -> filt
                        1 -> (filt + a) and 0xFF
                        2 -> (filt + b) and 0xFF
                        3 -> (filt + (a + b) / 2) and 0xFF
                        4 -> {
                            val p = a + b - c
                            val pa = abs(p - a)
                            val pb = abs(p - b)
                            val pc = abs(p - c)
                            val pr = if (pa <= pb && pa <= pc) a else if (pb <= pc) b else c
                            (filt + pr) and 0xFF
                        }
                        else -> filt
                    }
                    curRow[x] = recon.toByte()
                }
                prevRow = curRow

                for (x in 0 until width) {
                    when (bpp) {
                        4 -> { // RGBA
                            val r = curRow[x * 4].toInt() and 0xFF
                            val g = curRow[x * 4 + 1].toInt() and 0xFF
                            val b = curRow[x * 4 + 2].toInt() and 0xFF
                            val a = curRow[x * 4 + 3].toInt() and 0xFF
                            canvas.setPixel(x, y, r, g, b, a)
                        }
                        3 -> { // RGB
                            val r = curRow[x * 3].toInt() and 0xFF
                            val g = curRow[x * 3 + 1].toInt() and 0xFF
                            val b = curRow[x * 3 + 2].toInt() and 0xFF
                            canvas.setPixel(x, y, r, g, b, 255)
                        }
                        2 -> { // Grayscale + Alpha
                            val g = curRow[x * 2].toInt() and 0xFF
                            val a = curRow[x * 2 + 1].toInt() and 0xFF
                            canvas.setPixel(x, y, g, g, g, a)
                        }
                        1 -> { // Grayscale
                            val g = curRow[x].toInt() and 0xFF
                            canvas.setPixel(x, y, g, g, g, 255)
                        }
                    }
                }
            }

            return canvas
        }

        fun fromArgbArray(argb: IntArray, width: Int, height: Int): RgbaCanvas {
            require(argb.size == width * height)
            val canvas = RgbaCanvas(width, height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val color = argb[y * width + x]
                    val a = (color ushr 24) and 0xFF
                    val r = (color ushr 16) and 0xFF
                    val g = (color ushr 8) and 0xFF
                    val b = color and 0xFF
                    canvas.setPixel(x, y, r, g, b, a)
                }
            }
            return canvas
        }
    }
}

