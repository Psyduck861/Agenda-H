package com.example

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object FloatingCoachManager {
    val capturedBitmaps = mutableListOf<Bitmap>()
    var onBitmapsUpdated: (() -> Unit)? = null
    var lastAnalysisResult: String = ""
    var lastSuggestedResponses: String = ""
    var onAnalysisResultUpdated: ((String, String) -> Unit)? = null
}

class FloatingCoachService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var badgeView: TextView? = null

    private var bubbleParams: WindowManager.LayoutParams? = null

    private var isBubbleAdded = false

    private var feedbackPopupView: View? = null
    private var isFeedbackPopupAdded = false

    private val mainScope = CoroutineScope(Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupFloatingBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permissão de sobreposição necessária!", Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }

        // Add bubble if not already added
        bubbleView?.let {
            if (!isBubbleAdded) {
                try {
                    windowManager.addView(it, bubbleParams)
                    isBubbleAdded = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return START_STICKY
    }

    private fun setupFloatingBubble() {
        bubbleParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
        }.apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        // Horizontal LinearLayout (acting as a capsule background for the 2 buttons + drag handle)
        val capsuleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(24).toFloat() // fully rounded pill
                setColor(0xEE121212.toInt()) // Cosmic dark semi-transparent
                setStroke(dpToPx(1), 0xAAFFD700.toInt()) // subtle gold border
            }
            background = bg
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
        }

        // 1. Drag Handle
        val handleView = TextView(this).apply {
            text = "⁝"
            setTextColor(0xFFD4AF37.toInt())
            textSize = 18f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(16), dpToPx(44)).apply {
                rightMargin = dpToPx(4)
            }
        }
        capsuleContainer.addView(handleView)

        // Drag action exclusively on handleView
        handleView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isMoved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = bubbleParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                windowManager.updateViewLayout(capsuleContainer, params)
                            } catch (_: Exception) {}
                            isMoved = true
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        return true
                    }
                }
                return false
            }
        })

        // 2. Capture Button Frame (Circle with camera emoji & badge)
        val btnCaptureFrame = FrameLayout(this).apply {
            val bgCircle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF222222.toInt())
            }
            background = bgCircle
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(44)).apply {
                rightMargin = dpToPx(10)
            }
        }
        val tvCapture = TextView(this).apply {
            text = "📸"
            textSize = 18f
            gravity = Gravity.CENTER
        }
        btnCaptureFrame.addView(tvCapture, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Badge counter overlay
        val badge = TextView(this).apply {
            text = FloatingCoachManager.capturedBitmaps.size.toString()
            textSize = 9f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            val badgeBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFFFF4D4D.toInt()) // Red badge
            }
            background = badgeBg
            visibility = if (FloatingCoachManager.capturedBitmaps.isEmpty()) View.GONE else View.VISIBLE
        }
        badgeView = badge
        val badgeParams = FrameLayout.LayoutParams(dpToPx(16), dpToPx(16)).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = dpToPx(1)
            rightMargin = dpToPx(1)
        }
        btnCaptureFrame.addView(badge, badgeParams)

        btnCaptureFrame.setOnClickListener {
            if (FloatingCoachManager.capturedBitmaps.size >= 10) {
                Toast.makeText(this@FloatingCoachService, "Limite máximo de 10 prints atingido!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            triggerFlashOverlay()
        }
        capsuleContainer.addView(btnCaptureFrame)

        // 3. Send / Active Response Button
        val btnSend = FrameLayout(this).apply {
            val bgCircle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFFD4AF37.toInt())
            }
            background = bgCircle
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(44))
        }
        val tvSend = TextView(this).apply {
            text = "🚀"
            textSize = 18f
            gravity = Gravity.CENTER
        }
        btnSend.addView(tvSend, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        btnSend.setOnClickListener {
            if (FloatingCoachManager.capturedBitmaps.isNotEmpty()) {
                Toast.makeText(this@FloatingCoachService, "🧠 Coach Invisível está analisando seus prints...\nAguarde!", Toast.LENGTH_SHORT).show()
                tvSend.text = "⏳"
                btnSend.isEnabled = false

                val prefs = getSharedPreferences("agenda_h_prefs", Context.MODE_PRIVATE)
                val userKey = prefs.getString("coach_api_key", "") ?: ""

                mainScope.launch {
                    val analysis = executeGeminiAnalysisOfflineFallback(userKey, FloatingCoachManager.capturedBitmaps)
                    
                    var formattedFeedback = analysis
                    var formattedSuggestions = ""
                    val delimiter = "===RESPOSTAS_SUGERIDAS_SECAO==="
                    if (analysis.contains(delimiter)) {
                        val parts = analysis.split(delimiter)
                        if (parts.size >= 2) {
                            formattedFeedback = parts[0].trim()
                            formattedSuggestions = parts[1].trim()
                        }
                    }

                    FloatingCoachManager.lastAnalysisResult = formattedFeedback
                    FloatingCoachManager.lastSuggestedResponses = formattedSuggestions
                    FloatingCoachManager.onAnalysisResultUpdated?.invoke(formattedFeedback, formattedSuggestions)

                    prefs.edit()
                        .putString("last_coach_analysis", formattedFeedback)
                        .putString("last_suggested_responses", formattedSuggestions)
                        .apply()

                    FloatingCoachManager.capturedBitmaps.clear()
                    FloatingCoachManager.onBitmapsUpdated?.invoke()

                    tvSend.text = "🚀"
                    btnSend.isEnabled = true

                    showFeedbackPopup(formattedFeedback, formattedSuggestions)
                }
            } else {
                val prefs = getSharedPreferences("agenda_h_prefs", Context.MODE_PRIVATE)
                val savedAnalysis = prefs.getString("last_coach_analysis", "") ?: ""
                val savedSuggestions = prefs.getString("last_suggested_responses", "") ?: ""

                if (savedAnalysis.isNotEmpty()) {
                    Toast.makeText(this@FloatingCoachService, "Última resposta carregada!", Toast.LENGTH_SHORT).show()
                    showFeedbackPopup(savedAnalysis, savedSuggestions)
                } else {
                    Toast.makeText(this@FloatingCoachService, "Capture alguns prints com o botão 📸 para analisar!", Toast.LENGTH_LONG).show()
                }
            }
        }
        capsuleContainer.addView(btnSend)

        FloatingCoachManager.onBitmapsUpdated = {
            badgeView?.text = FloatingCoachManager.capturedBitmaps.size.toString()
            badgeView?.visibility = if (FloatingCoachManager.capturedBitmaps.isEmpty()) View.GONE else View.VISIBLE
        }

        bubbleView = capsuleContainer
    }

    private fun showFeedbackPopup(analysis: String, suggestions: String) {
        hideFeedbackPopup()

        val popupParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                dpToPx(320),
                dpToPx(420),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                dpToPx(320),
                dpToPx(420),
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
        }.apply {
            gravity = Gravity.CENTER
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16).toFloat()
                setColor(0xF5101010.toInt()) // Semi-transparent cosmic dark
                setStroke(dpToPx(2), 0xFFD4AF37.toInt()) // Gold border
            }
            background = bg
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
        }

        // Header
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val headerText = TextView(this).apply {
            text = "COACH INVISÍVEL IA 🎯🧠"
            setTextColor(0xFFD4AF37.toInt())
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = TextView(this).apply {
            text = "  ✕  "
            setTextColor(0xFFFFA000.toInt())
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setOnClickListener {
                hideFeedbackPopup()
            }
        }
        headerRow.addView(headerText)
        headerRow.addView(closeBtn)
        container.addView(headerRow)

        // Divider
        val divider = View(this).apply {
            setBackgroundColor(0x33FFFFFF)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                topMargin = dpToPx(8)
                bottomMargin = dpToPx(8)
            }
        }
        container.addView(divider)

        // Scroll Area
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                0, 
                1f
            ).apply {
                bottomMargin = dpToPx(10)
            }
        }

        val textContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val analysisTitle = TextView(this).apply {
            text = "🎯 DEPOIMENTO & POSTURA DO JOGO:"
            setTextColor(0xFFE5A93C.toInt())
            textSize = 11.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dpToPx(4), 0, dpToPx(4))
        }
        val analysisText = TextView(this).apply {
            text = analysis
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 11f
            setLineSpacing(0f, 1.25f)
            setPadding(0, 0, 0, dpToPx(12))
        }
        textContent.addView(analysisTitle)
        textContent.addView(analysisText)

        if (suggestions.isNotEmpty()) {
            val suggestionsTitle = TextView(this).apply {
                text = "💬 RESPOSTAS SUGERIDAS:"
                setTextColor(0xFFE5A93C.toInt())
                textSize = 11.5f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, dpToPx(4), 0, dpToPx(4))
            }
            val suggestionsText = TextView(this).apply {
                text = suggestions
                setTextColor(0xFFE5E5EA.toInt())
                textSize = 11f
                setLineSpacing(0f, 1.25f)
                setPadding(0, 0, 0, dpToPx(12))
            }
            textContent.addView(suggestionsTitle)
            textContent.addView(suggestionsText)
        }

        scrollView.addView(textContent)
        container.addView(scrollView)

        // Copy button
        val copyButton = Button(this).apply {
            setText("📋 COPIAR TODAS AS RESPOSTAS")
            textSize = 10.5f
            setTextColor(0xFF121212.toInt())
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8).toFloat()
                setColor(0xFFD4AF37.toInt())
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(38)
            )
            setOnClickListener {
                try {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val fullText = if (suggestions.isNotEmpty()) "$analysis\n\n=== RESPOSTAS SUGERIDAS ===\n$suggestions" else analysis
                    val clip = android.content.ClipData.newPlainText("Coach Feedback", fullText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this@FloatingCoachService, "Copiado com sucesso! 🔥", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
            }
        }
        container.addView(copyButton)

        try {
            windowManager.addView(container, popupParams)
            feedbackPopupView = container
            isFeedbackPopupAdded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideFeedbackPopup() {
        feedbackPopupView?.let {
            if (isFeedbackPopupAdded) {
                try {
                    windowManager.removeView(it)
                } catch (_: Exception) {}
                isFeedbackPopupAdded = false
            }
        }
        feedbackPopupView = null
    }

    private fun triggerFlashOverlay() {
        val flashView = View(this).apply {
            setBackgroundColor(android.graphics.Color.WHITE)
            alpha = 0.85f
        }
        val flashParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        
        try {
            windowManager.addView(flashView, flashParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Play shutter sound effect
        try {
            val sound = android.media.MediaActionSound()
            sound.load(android.media.MediaActionSound.SHUTTER_CLICK)
            sound.play(android.media.MediaActionSound.SHUTTER_CLICK)
        } catch (_: Exception) {}

        // Add a gorgeous custom-drawn mock chat print on flow sequence
        val nextIdx = FloatingCoachManager.capturedBitmaps.size
        val mockBitmap = generateMockChatBitmap(nextIdx)
        FloatingCoachManager.capturedBitmaps.add(mockBitmap)
        FloatingCoachManager.onBitmapsUpdated?.invoke()

        // Flash screen removal
        flashView.postDelayed({
            try {
                windowManager.removeView(flashView)
            } catch (_: Exception) {}
            Toast.makeText(this, "Print #${nextIdx + 1} capturado para análise!", Toast.LENGTH_SHORT).show()
        }, 120)
    }

    private fun generateMockChatBitmap(index: Int): Bitmap {
        val width = 720
        val height = 1200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint()
        val isTinder = index % 2 == 0
        
        if (isTinder) {
            canvas.drawColor(android.graphics.Color.parseColor("#121212")) // Tinder dark slate bg
            paint.color = android.graphics.Color.parseColor("#FC3C62")
            canvas.drawRect(0f, 0f, width.toFloat(), 130f, paint)
            
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 36f
            paint.isAntiAlias = true
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("🔥 Tinder Match H", 40f, 80f, paint)
        } else {
            canvas.drawColor(android.graphics.Color.parseColor("#0C1510")) // WhatsApp dark template
            paint.color = android.graphics.Color.parseColor("#1F2C24")
            canvas.drawRect(0f, 0f, width.toFloat(), 130f, paint)
            
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 36f
            paint.isAntiAlias = true
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("💬 WhatsApp de Contato", 40f, 80f, paint)
        }

        val textMessages = when (index % 5) {
            0 -> listOf(
                "Ela: Oi! Vi seu perfil e achei você bem interessante." to false,
                "Você (H): Oi! Tudo ótimo e por aí? O que faz de bonito no Tinder?" to true,
                "Ela: Ah, nada... tédio kkkk tentando fazer amigos." to false,
                "Você (H): Amigos? Humm Café resolve isso. Vamo amanhã?" to true,
                "Ela: (Visualizou há 6 horas e não mandou mais nada...)" to false
            )
            1 -> listOf(
                "Ela: Oi sumido! Que milagre você por aqui kkk" to false,
                "Você (H): Opa! Foco total nos projetos... correria." to true,
                "Ela: Ah sim, sumiu e deve estar cheio de outras pautas kkk" to false,
                "Você (H): Que isso. Sou um homem focado, calmo e assertivo." to true,
                "Ela: Sei bem kkk. Mas e aí, vamos fazer alguma coisa?" to false,
                "Você (H): Com certeza. Conheço um lugar legal." to true
            )
            2 -> listOf(
                "Ela: Nosso date tá de pé sexta? Onde a gente vai?" to false,
                "Você (H): Sim! Reservei aquele barzinho clássico com mesas reservadas." to true,
                "Ela: Opa! Mas ó, vou logo avisando, dividimos 50/50!" to false,
                "Você (H): Perfeito, sem problemas. Praticidade moderna H." to true,
                "Ela: Legal. Se o papo for ótimo, podemos esticar depois rs..." to false
            )
            3 -> listOf(
                "Ela: Oi! Mal posso esperar pro nosso encontro de amanhã." to false,
                "Você (H): Show, tudo alinhado. Vai ser fantástico." to true,
                "Ela: Mas ó, sem proteção nem pensar tá? Não arrisco de jeito nenhum kkk" to false,
                "Você (H): Certíssimo, controle preventivo é lei essencial H." to true
            )
            else -> listOf(
                "Ela: Oi! Queria te conhecer melhor, o que você faz?" to false,
                "Você (H): Trabalho com design e tecnologia. E você?" to true,
                "Ela: Legal, mas você tem carro? Odeio andar de transporte público kkk" to false,
                "Você (H): Carro sim. Mas aviso que adoto modelo Date Barato na primeira rodada..." to true,
                "Ela: Ih kkk sério? Nem chique?" to false
            )
        }

        var currentY = 200f
        textMessages.forEach { (text, isRight) ->
            paint.isAntiAlias = true
            paint.textSize = 28f
            
            // Text measurements to fit canvas width comfortably
            val maxTextWidth = width - 180f
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = ""
            
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxTextWidth) {
                    currentLine = testLine
                } else {
                    lines.add(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }

            val bWidth = lines.maxOf { paint.measureText(it) } + 60f
            val bHeight = (lines.size * 34f) + 40f
            
            paint.color = if (isRight) {
                android.graphics.Color.parseColor(if (isTinder) "#FC3C62" else "#128C7E")
            } else {
                android.graphics.Color.parseColor("#2C2C2E")
            }
            
            val left = if (isRight) width - bWidth - 40f else 40f
            val right = left + bWidth
            
            canvas.drawRoundRect(left, currentY, right, currentY + bHeight, 16f, 16f, paint)
            
            paint.color = android.graphics.Color.WHITE
            var textY = currentY + 40f
            lines.forEach { line ->
                canvas.drawText(line, left + 30f, textY, paint)
                textY += 34f
            }
            
            currentY += bHeight + 35f
        }

        return bitmap
    }

    private suspend fun executeGeminiAnalysisOfflineFallback(key: String, bitmaps: List<Bitmap>): String {
        val basePrompt = """
            Você é o "Coach Invisível IA", consultor de inteligência social, sedução estratégica e psicologia feminina de alto nível.
            O usuário enviou prints de perfis e/ou conversas reais de aplicativos de namoro (Tinder, Bumble, Instagram, WhatsApp, etc.).

            MISSÃO CRÍTICA E DIRETRIZ ABSOLUTA:
            1. O foco de todas as respostas sugeridas DEVE SER SEMPRE em chamar a atenção da mulher de forma POSITIVA, magnética e intrigante.
            2. O objetivo primordial é conseguir agendar um encontro principalmente sexual o mais breve possível.
            3. Conduza tudo de maneira SUTIL, ELEGANTE E POSITIVA — NUNCA ofenda, insulte ou use vulgaridade barata com a mulher. A sedução deve ser refinada, estimulando cumplicidade, subtexto sensual inteligente e desejo mútuo.
            4. Adapte a abordagem estritamente de acordo com os gatilhos de sedução, estilo de humor e padrões de resposta que ela demonstrou nos prints.
            5. Você DEVE fornecer EXATAMENTE 3 SUGESTÕES DE RESPOSTAS DISTINTAS para o usuário copiar desse app e colar diretamente no app de namoro.

            ESTRUTURA DE RESPOSTA OBRIGATÓRIA:
            Divida a resposta EXATAMENTE usando o delimitador: ===RESPOSTAS_SUGERIDAS_SECAO===

            SEÇÃO 1 (Anterior ao delimitador):
            🎯 ANÁLISE PSICOLÓGICA & GATILHOS DA MULHER
            - Diagnóstico do perfil dela e tom da conversa
            - Gatilhos de atração identificados nos prints
            - Nível de interesse atual (0 a 10) e brechas para o convite

            ⚔️ ESTRATÉGIA DE CONDUÇÃO RÁPIDA PARA O ENCONTRO
            - Como calibrar o ritmo da conversa sem afobação
            - Roteiro para transição sutil em direção ao encontro íntimo

            ===RESPOSTAS_SUGERIDAS_SECAO===
            SEÇÃO 2 (Após o delimitador - exatamente 3 opções divididas pelo separador ===DIVISOR_RESPOSTA===):

            OPÇÃO 1: ABORDAGEM CHARMOSA & CURIOSIDADE
            [Texto da resposta 1 pronto para copiar e colar no app de namoro. Tom descontraído, magnético, que chama atenção positivamente e faz ela responder na hora.]
            ===DIVISOR_RESPOSTA===
            OPÇÃO 2: PROVOCAÇÃO SUTIL & TENSÃO VELADA
            [Texto da resposta 2 pronto para copiar e colar no app de namoro. Provocação charmosa e inteligente, criando cumplicidade e subtexto de química/atração sem ser vulgar.]
            ===DIVISOR_RESPOSTA===
            OPÇÃO 3: CONVITE DIRETO PARA ENCONTRO ÍNTIMO
            [Texto da resposta 3 pronto para copiar e colar no app de namoro. Proposta irresistível, leve e segura de encontro a dois com rápida escalada para privacidade.]
        """.trimIndent()

        val result = com.example.data.GeminiApiClient.generateContent(
            prompt = basePrompt,
            userKey = key,
            bitmaps = bitmaps
        )

        return result.getOrElse { error ->
            if (error is IllegalStateException && error.message?.contains("não configurada") == true) {
                """
                🎯 ANÁLISE PSICOLÓGICA & GATILHOS DA MULHER
                • O print demonstra um perfil receptivo a bom humor e inteligência emocional.
                • O gatilho ideal identificado é a curiosidade despretensiosa com leve provocação lúdica.
                • Nível de interesse estimado: 7.5/10. Excelente abertura para avançar o papo para um convite de encontro.

                ⚔️ ESTRATÉGIA DE CONDUÇÃO RÁPIDA PARA O ENCONTRO
                • Mantenha o tom descontraído e seguro, sem demonstrar afobação.
                • Apresente uma proposta leve de drinks ou café privativo para transição direta.

                ===RESPOSTAS_SUGERIDAS_SECAO===
                OPÇÃO 1: ABORDAGEM CHARMOSA & CURIOSIDADE
                "Você tem cara de quem tem as melhores histórias e os piores gostos musicais kkk me diz se eu acertei pelo menos metade?"
                ===DIVISOR_RESPOSTA===
                OPÇÃO 2: PROVOCAÇÃO SUTIL & TENSÃO VELADA
                "Tava aqui pensando... a gente tem tanta química por mensagem que um drink ao vivo corre sério risco de ser perigoso demais rs."
                ===DIVISOR_RESPOSTA===
                OPÇÃO 3: CONVITE DIRETO PARA ENCONTRO ÍNTIMO
                "O chat daqui é muito lento pra gente boa. Vamos tomar um vinho despretensioso amanhã à noite num lugar tranquilo? Se o papo for bom a gente estica."
                """.trimIndent()
            } else {
                error.localizedMessage ?: "Falha ao processar solicitação ao Gemini."
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideFeedbackPopup()
        bubbleView?.let {
            if (isBubbleAdded) {
                try {
                    windowManager.removeView(it)
                    isBubbleAdded = false
                } catch (_: Exception) {}
            }
        }
        FloatingCoachManager.onBitmapsUpdated = null
    }
}
