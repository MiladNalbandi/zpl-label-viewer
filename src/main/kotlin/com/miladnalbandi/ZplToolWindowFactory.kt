package com.miladnalbandi

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.intellij.ui.content.ContentFactory
import com.miladnalbandi.zpl.ZplRenderer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.image.BufferedImage
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ZplToolWindowFactory : ToolWindowFactory {

    private val log = Logger.getInstance(ZplToolWindowFactory::class.java)

    // ── Input-type choices ────────────────────────────────────────────────────
    private val INPUT_AUTO = "Auto-detect"
    private val INPUT_ZPL  = "ZPL"
    private val INPUT_B64  = "Base-64"

    // ── Render-source choices ─────────────────────────────────────────────────
    private val SRC_LOCAL          = "Local renderer"
    private val SRC_API            = "Labelary API"
    private val SRC_LOCAL_THEN_API = "Local → API fallback"

    // ─────────────────────────────────────────────────────────────────────────

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = SimpleToolWindowPanel(false)

        // ── Header ────────────────────────────────────────────────────────────
        val header = JBLabel("ZPL Decoder").apply {
            font = font.deriveFont(Font.BOLD, 16f)
            border = JBUI.Borders.emptyBottom(6)
        }

        // ── Text input ────────────────────────────────────────────────────────
        val input = JBTextArea(7, 44).apply {
            lineWrap = true; wrapStyleWord = true
            font = Font("Monospaced", Font.PLAIN, 12)
        }
        val inputScroll = JBScrollPane(input).apply {
            border = CompoundBorder(
                JBUI.Borders.customLine(JBColor.border(), 1),
                JBUI.Borders.empty(2)
            )
        }
        val inputPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty()
            add(JBLabel("Input").apply {
                border = JBUI.Borders.emptyBottom(4)
                font = font.deriveFont(Font.BOLD)
            }, BorderLayout.NORTH)
            add(inputScroll, BorderLayout.CENTER)
        }

        // ── Controls row ──────────────────────────────────────────────────────
        val dpiCombo = ComboBox(arrayOf("200 dpi", "300 dpi")).apply { selectedIndex = 1 }

        val inputTypeCombo = ComboBox(arrayOf(INPUT_AUTO, INPUT_ZPL, INPUT_B64)).apply {
            selectedIndex = 0
            toolTipText = "Select the input format, or use Auto-detect"
        }

        val sourceCombo = ComboBox(arrayOf(SRC_LOCAL, SRC_LOCAL_THEN_API, SRC_API)).apply {
            selectedIndex = 0
            toolTipText = "Choose rendering engine"
        }

        val aaCheck = JCheckBox("Antialias").apply { isOpaque = false }

        val controlsRow = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            isOpaque = false
            add(JBLabel("DPI:")); add(dpiCombo)
            add(JSeparator(SwingConstants.VERTICAL).apply { preferredSize = Dimension(1, 22) })
            add(JBLabel("Input:")); add(inputTypeCombo)
            add(JSeparator(SwingConstants.VERTICAL).apply { preferredSize = Dimension(1, 22) })
            add(JBLabel("Source:")); add(sourceCombo)
            add(JSeparator(SwingConstants.VERTICAL).apply { preferredSize = Dimension(1, 22) })
            add(aaCheck)
        }

        // ── Action buttons ────────────────────────────────────────────────────
        val btnRender = styledButton("Render", accent = true, tip = "Render the label using the selected input type and source")
        val btnSave   = styledButton("Save PNG",    tip = "Save the rendered image as PNG").apply { isEnabled = false }
        val btnShowZpl = styledButton("View ZPL",   tip = "Show the decoded ZPL text")
        val btnClearCache = styledButton("Clear Cache", tip = "Flush in-memory image caches")

        val buttonRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            isOpaque = false
            add(btnRender); add(btnSave); add(btnShowZpl); add(btnClearCache)
        }

        // ── Preview — scales image to fit, no scrollbars needed ──────────────
        val imageCanvas = object : JPanel() {
            var image: BufferedImage? = null
            var zoomLabel = ""

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val img = image
                if (img == null) {
                    g.color = foreground
                    val fm = g.fontMetrics
                    val msg = "No image yet"
                    g.drawString(msg, (width - fm.stringWidth(msg)) / 2, height / 2)
                    return
                }
                // Scale to fit, maintaining aspect ratio, with 8 px padding
                val pad  = 8
                val avW  = (width  - pad * 2).coerceAtLeast(1)
                val avH  = (height - pad * 2).coerceAtLeast(1)
                val scale = minOf(avW.toDouble() / img.width, avH.toDouble() / img.height, 1.0)
                val dw   = (img.width  * scale).toInt()
                val dh   = (img.height * scale).toInt()
                val dx   = (width  - dw) / 2
                val dy   = (height - dh) / 2
                (g as Graphics2D).apply {
                    setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                    drawImage(img, dx, dy, dw, dh, null)
                }
                zoomLabel = if (scale >= 1.0) "100 %" else "${(scale * 100).toInt()} %"
            }

            override fun getPreferredSize() = Dimension(520, 380)
        }.apply {
            background = JBColor(Color(0xF5F5F5), Color(0x2B2B2B))
            foreground = JBColor.GRAY
            border = JBUI.Borders.customLine(JBColor.border(), 1)
        }

        val previewPanel = JPanel(BorderLayout(0, 4)).apply {
            border = JBUI.Borders.empty()
            add(JBLabel("Preview").apply {
                border = JBUI.Borders.emptyBottom(2)
                font = font.deriveFont(Font.BOLD)
            }, BorderLayout.NORTH)
            add(imageCanvas, BorderLayout.CENTER)
        }

        // ── Status bar ────────────────────────────────────────────────────────
        val statusBar = JBLabel("Ready").apply {
            foreground = JBColor.GRAY
            border = JBUI.Borders.empty(4, 2, 2, 2)
            font = font.deriveFont(11f)
        }

        // ── Main layout ───────────────────────────────────────────────────────
        val top = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8)
            isOpaque = false
            add(header)
            add(inputPanel)
            add(Box.createRigidArea(Dimension(0, 8)))
            add(controlsRow)
            add(Box.createRigidArea(Dimension(0, 6)))
            add(buttonRow)
            add(Box.createRigidArea(Dimension(0, 8)))
        }

        panel.setContent(JPanel(BorderLayout(0, 0)).apply {
            border = JBUI.Borders.empty(0, 4, 4, 4)
            add(top, BorderLayout.NORTH)
            add(previewPanel, BorderLayout.CENTER)
            add(statusBar, BorderLayout.SOUTH)
        })

        toolWindow.contentManager.addContent(
            ContentFactory.getInstance().createContent(panel, "", false)
        )

        // ── State ─────────────────────────────────────────────────────────────
        var imgCurrent: BufferedImage? = null

        fun dpi() = if (dpiCombo.selectedIndex == 0) 200 else 300

        fun setStatus(msg: String, isError: Boolean = false) {
            statusBar.foreground = if (isError) JBColor.RED else JBColor.GRAY
            statusBar.text = msg
        }

        fun showImage(img: BufferedImage) {
            imgCurrent = img
            imageCanvas.image = img
            imageCanvas.repaint()
            btnSave.isEnabled = true
        }

        fun popup(title: String, text: String) = JOptionPane.showMessageDialog(
            panel,
            JBScrollPane(JBTextArea(text).apply {
                isEditable = false; lineWrap = false
                font = Font("Monospaced", Font.PLAIN, 12)
            }).apply { preferredSize = Dimension(640, 400) },
            title, JOptionPane.PLAIN_MESSAGE
        )

        // ── Auto-detect input type ─────────────────────────────────────────────
        fun detectType(raw: String): String {
            val t = raw.trim()
            if (t.startsWith("^") || t.contains("^XA", ignoreCase = true)) return INPUT_ZPL
            runCatching {
                val decoded = String(Base64.getDecoder().decode(t), StandardCharsets.UTF_8)
                if (decoded.contains("^XA", ignoreCase = true) || decoded.startsWith("^"))
                    return INPUT_B64
            }
            return INPUT_ZPL   // default assumption
        }

        input.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateDetect()
            override fun removeUpdate(e: DocumentEvent?) = updateDetect()
            override fun changedUpdate(e: DocumentEvent?) {}
            fun updateDetect() {
                if (inputTypeCombo.selectedItem == INPUT_AUTO) {
                    val detected = detectType(input.text)
                    setStatus("Auto-detected: $detected")
                }
            }
        })

        // ── Resolve ZPL from current input ────────────────────────────────────
        fun resolveZpl(): String? {
            val raw = input.text.trim()
            if (raw.isEmpty()) { setStatus("Input is empty", true); return null }

            val type = when (inputTypeCombo.selectedItem as String) {
                INPUT_AUTO -> detectType(raw)
                else       -> inputTypeCombo.selectedItem as String
            }

            return when (type) {
                INPUT_B64 -> runCatching {
                    String(Base64.getDecoder().decode(raw), StandardCharsets.UTF_8)
                }.getOrElse { setStatus("Invalid Base-64", true); null }
                else -> raw
            }
        }

        // ── Render helpers ────────────────────────────────────────────────────
        fun renderLocal(zpl: String): BufferedImage? = runCatching {
            ZplRenderer.render(zpl, dpi(), antialias = aaCheck.isSelected)
        }.getOrNull()

        fun renderApi(zpl: String): BufferedImage? = fetchLabelary(zpl, dpi())

        // ── Render action ─────────────────────────────────────────────────────
        fun doRender(@Suppress("UNUSED_PARAMETER") e: ActionEvent) {
            val zpl = resolveZpl() ?: return

            val source = sourceCombo.selectedItem as String
            setStatus("Rendering…")
            btnRender.isEnabled = false

            val img: BufferedImage? = when (source) {
                SRC_LOCAL -> {
                    renderLocal(zpl).also {
                        if (it == null) setStatus("Local rendering produced no output", true)
                    }
                }
                SRC_API -> {
                    renderApi(zpl).also {
                        if (it == null) setStatus("Labelary API returned no image", true)
                    }
                }
                SRC_LOCAL_THEN_API -> {
                    val local = renderLocal(zpl)
                    if (local != null) {
                        local
                    } else {
                        setStatus("Local failed — trying Labelary API…")
                        renderApi(zpl).also {
                            if (it == null) setStatus("Both local and API rendering failed", true)
                        }
                    }
                }
                else -> null
            }

            btnRender.isEnabled = true

            if (img != null) {
                showImage(img)
                setStatus("Rendered  ${img.width} × ${img.height} px  —  zoom ${imageCanvas.zoomLabel}")
            }
        }

        btnRender.addActionListener(::doRender)

        // ── Save ──────────────────────────────────────────────────────────────
        btnSave.addActionListener {
            val img = imgCurrent ?: return@addActionListener
            val fc = JFileChooser().apply { selectedFile = File("label.png") }
            if (fc.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
                var f = fc.selectedFile
                if (!f.name.endsWith(".png")) f = File(f.path + ".png")
                runCatching { ImageIO.write(img, "png", f) }
                    .onSuccess { setStatus("Saved → ${f.absolutePath}") }
                    .onFailure { setStatus("Save failed: ${it.message}", true) }
            }
        }

        // ── View ZPL ──────────────────────────────────────────────────────────
        btnShowZpl.addActionListener {
            val zpl = resolveZpl() ?: return@addActionListener
            popup("ZPL", zpl)
        }

        // ── Clear cache ───────────────────────────────────────────────────────
        btnClearCache.addActionListener {
            ZplRenderer.clearCache()
            com.miladnalbandi.zpl.LocalZplGraphicDecoder.clearCache()
            setStatus("Caches cleared")
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    /**
     * Creates a button styled for the IntelliJ platform.
     * [accent] = true gives it a filled/primary style.
     */
    private fun styledButton(label: String, accent: Boolean = false, tip: String? = null): JButton {
        return JButton(label).apply {
            toolTipText = tip
            isFocusPainted = false
            if (accent) {
                background = JBColor(Color(0x4A90D9), Color(0x4A90D9))
                foreground = Color.WHITE
                font = font.deriveFont(Font.BOLD)
            }
            border = CompoundBorder(
                JBUI.Borders.customLine(
                    if (accent) JBColor(Color(0x3A7AC8), Color(0x3A7AC8)) else JBColor.border(),
                    1
                ),
                JBUI.Borders.empty(4, 12)
            )
            cursor = Cursor(Cursor.HAND_CURSOR)
        }
    }

    // ── Labelary API ──────────────────────────────────────────────────────────

    private fun fetchLabelary(zpl: String, dpi: Int): BufferedImage? = runCatching {
        val dpmm = if (dpi == 200) 8 else 12
        val url  = "https://api.labelary.com/v1/printers/${dpmm}dpmm/labels/4x6/0/"

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val req = Request.Builder().url(url)
            .post(RequestBody.create("application/x-www-form-urlencoded".toMediaType(), zpl.toByteArray()))
            .build()

        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) { log.warn("Labelary ${res.code}"); return null }
            ImageIO.read(res.body!!.byteStream())
        }
    }.getOrNull()
}
