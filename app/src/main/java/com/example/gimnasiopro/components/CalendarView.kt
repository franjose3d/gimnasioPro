package com.example.gimnasiopro.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.example.gimnasiopro.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Componente de calendario reutilizable.
 * Sigue el principio SOLID de Single Responsibility.
 * Este componente solo se encarga de mostrar y manejar el calendario.
 */
class CalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val tvMonthYear: TextView
    private val btnPreviousMonth: ImageButton
    private val btnNextMonth: ImageButton
    private val gridCalendar: GridLayout

    private val calendar: Calendar = Calendar.getInstance()
    private var selectedDay: Int = -1
    private var onDateSelectedListener: OnDateSelectedListener? = null

    // ✅ NUEVO: Para saber qué días de la semana tienen rutina
    private var diasConRutina: Set<Int> = emptySet()  // 1=Lunes, 2=Martes, etc.
    private var onDateLongPressListener: OnDateLongPressListener? = null

    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))

    interface OnDateSelectedListener {
        fun onDateSelected(year: Int, month: Int, day: Int)
    }

    // ✅ NUEVO: Interface para long press
    interface OnDateLongPressListener {
        fun onDateLongPress(year: Int, month: Int, day: Int, diaSemana: Int)
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_calendar, this, true)

        tvMonthYear = findViewById(R.id.tvMonthYear)
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        gridCalendar = findViewById(R.id.gridCalendar)

        setupNavigation()
        updateCalendar()
    }

    private fun setupNavigation() {
        btnPreviousMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }
    }

    private fun updateCalendar() {
        // Update month/year header
        tvMonthYear.text = monthYearFormat.format(calendar.time)
            .replaceFirstChar { it.uppercase() }

        // Clear previous days
        gridCalendar.removeAllViews()

        // Get calendar info
        val tempCalendar = calendar.clone() as Calendar
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Adjust for Monday start (Spanish calendar)
        val startOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

        // Add empty cells for days before first day of month
        for (i in 0 until startOffset) {
            addDayCell("", false)
        }

        // Add day cells
        val today = Calendar.getInstance()
        val isCurrentMonth = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)
        val currentDay = today.get(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            val isToday = isCurrentMonth && day == currentDay
            addDayCell(day.toString(), true, day, isToday)
        }
    }

    private fun addDayCell(
        text: String,
        isClickable: Boolean,
        day: Int = 0,
        isToday: Boolean = false
    ) {
        val textView = TextView(context).apply {
            this.text = text
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setPadding(8, 16, 8, 16)

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            layoutParams = params

            if (isClickable && text.isNotEmpty()) {
                // ✅ Calcular día de la semana
                val tempCal = calendar.clone() as Calendar
                tempCal.set(Calendar.DAY_OF_MONTH, day)
                val dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
                val diaSemana = when (dayOfWeek) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    Calendar.SUNDAY -> 7
                    else -> 1
                }

                val tieneRutina = diasConRutina.contains(diaSemana)

                // Click normal
                setOnClickListener {
                    selectedDay = day
                    updateCalendar()
                    onDateSelectedListener?.onDateSelected(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        day
                    )
                }

                // ✅ NUEVO: Long press SOLO si tiene rutina
                if (tieneRutina) {
                    var longPressHandler: Runnable? = null

                    setOnTouchListener { v, event ->
                        when (event.action) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                longPressHandler = Runnable {
                                    // Vibrar para feedback
                                    val vibrator =
                                        context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        vibrator.vibrate(
                                            android.os.VibrationEffect.createOneShot(
                                                50,
                                                android.os.VibrationEffect.DEFAULT_AMPLITUDE
                                            )
                                        )
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(50)
                                    }

                                    onDateLongPressListener?.onDateLongPress(
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        day,
                                        diaSemana
                                    )
                                }
                                postDelayed(longPressHandler, 2000) // 2 segundos
                            }

                            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                                longPressHandler?.let { removeCallbacks(it) }
                            }
                        }
                        false // No consumir el evento, permitir click normal
                    }
                }
            }

            // ✅ Styling - Añadir color para días con rutina
            when {
                isToday -> {
                    setBackgroundResource(R.drawable.calendar_today_background)
                    setTextColor(resources.getColor(R.color.text_on_button, null))
                }

                day == selectedDay -> {
                    setBackgroundResource(R.drawable.calendar_selected_background)
                    setTextColor(resources.getColor(R.color.text_on_button, null))
                }

                text.isNotEmpty() -> {
                    // ✅ NUEVO: Color diferente si tiene rutina
                    val tempCal = calendar.clone() as Calendar
                    tempCal.set(Calendar.DAY_OF_MONTH, day)
                    val dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
                    val diaSemana = when (dayOfWeek) {
                        Calendar.MONDAY -> 1
                        Calendar.TUESDAY -> 2
                        Calendar.WEDNESDAY -> 3
                        Calendar.THURSDAY -> 4
                        Calendar.FRIDAY -> 5
                        Calendar.SATURDAY -> 6
                        Calendar.SUNDAY -> 7
                        else -> 1
                    }

                    if (diasConRutina.contains(diaSemana)) {
                        // Color para días CON rutina (azul/verde)
                        setTextColor(resources.getColor(R.color.primary, null))
                    } else {
                        // Color normal
                        setTextColor(resources.getColor(R.color.text_on_background, null))
                    }
                }
            }
        }
        gridCalendar.addView(textView)
    }

    fun setOnDateSelectedListener(listener: OnDateSelectedListener) {
        this.onDateSelectedListener = listener
    }

    fun setSelectedDate(year: Int, month: Int, day: Int) {
        calendar.set(year, month, day)
        selectedDay = day
        updateCalendar()
    }

    fun getCurrentYear(): Int = calendar.get(Calendar.YEAR)
    fun getCurrentMonth(): Int = calendar.get(Calendar.MONTH)
    fun getSelectedDay(): Int = selectedDay

    // ✅ NUEVO: Setear qué días de la semana tienen rutina
    fun setDiasConRutina(dias: Set<Int>) {
        this.diasConRutina = dias
        updateCalendar() // Refrescar para mostrar los cambios
    }

    // ✅ NUEVO: Listener para long press
    fun setOnDateLongPressListener(listener: OnDateLongPressListener) {
        this.onDateLongPressListener = listener
    }
}