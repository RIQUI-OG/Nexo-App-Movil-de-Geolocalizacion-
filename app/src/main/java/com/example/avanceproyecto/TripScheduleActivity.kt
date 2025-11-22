package com.example.avanceproyecto

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class TripScheduleActivity : AppCompatActivity() {

    private lateinit var seekBarTripType: SeekBar
    private lateinit var tvTripTypeValue: TextView
    private lateinit var btnDatePicker: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnTimePicker: Button
    private lateinit var tvSelectedTime: TextView
    private lateinit var etDestination: EditText
    private lateinit var btnScheduleTripConfirm: Button
    private lateinit var ratingBarSecurity: RatingBar
    private lateinit var tvSecurityValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_schedule)

        initViews()
        setupSeekBar()
        setupDatePicker()
        setupTimePicker()
        setupRatingBar()
        setupScheduleButton()
    }

    private fun initViews() {
        seekBarTripType = findViewById(R.id.seekBarTripType)
        tvTripTypeValue = findViewById(R.id.tvTripTypeValue)
        btnDatePicker = findViewById(R.id.btnDatePicker)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        btnTimePicker = findViewById(R.id.btnTimePicker)
        tvSelectedTime = findViewById(R.id.tvSelectedTime)
        etDestination = findViewById(R.id.etDestination)
        btnScheduleTripConfirm = findViewById(R.id.btnScheduleTripConfirm)
        ratingBarSecurity = findViewById(R.id.ratingBarSecurity)
        tvSecurityValue = findViewById(R.id.tvSecurityValue)
    }

    private fun setupSeekBar() {
        seekBarTripType.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val tripTypes = arrayOf("Corta Distancia", "Viaje Regular", "Viaje Largo", "Viaje Internacional")
                tvTripTypeValue.text = tripTypes[progress]
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupDatePicker() {
        btnDatePicker.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "${selectedDay}/${selectedMonth + 1}/${selectedYear}"
                tvSelectedDate.text = selectedDate
            }, year, month, day).show()
        }
    }

    private fun setupTimePicker() {
        btnTimePicker.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                tvSelectedTime.text = formattedTime
            }, hour, minute, true).show()
        }
    }

    private fun setupRatingBar() {
        // El RatingBar ahora se actualiza automáticamente
        ratingBarSecurity.setOnRatingBarChangeListener { _, rating, _ ->
            val securityLevels = arrayOf("Básica", "Media", "Alta", "Muy Alta", "Máxima")
            val securityText = if (rating < 1) "Básica" else securityLevels[rating.toInt() - 1]
            tvSecurityValue.text = "Seguridad: $securityText"
        }
    }

    private fun setupScheduleButton() {
        btnScheduleTripConfirm.setOnClickListener {
            val destination = etDestination.text.toString()
            val tripType = tvTripTypeValue.text.toString()
            val date = tvSelectedDate.text.toString()
            val time = tvSelectedTime.text.toString()
            val securityLevel = tvSecurityValue.text.toString()

            if (destination.isNotEmpty() && date != "No seleccionada" && time != "No seleccionada") {
                val message = "✅ Viaje programado:\n" +
                        "Destino: $destination\n" +
                        "Tipo: $tripType\n" +
                        "Salida: $date a las $time\n" +
                        "$securityLevel"

                Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                // Enviar notificación a la familia
                sendTripNotification(destination, tripType, "$date a las $time", securityLevel)

                finish() // Volver al Home
            } else {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendTripNotification(destination: String, tripType: String, schedule: String, security: String) {
        // Aquí implementarías la notificación a familiares
        // Por ahora solo un Toast de ejemplo
        Toast.makeText(this,
            "📧 Notificación enviada a tu familia:\n" +
                    "Viaje a: $destination\n" +
                    "Tipo: $tripType\n" +
                    "Horario: $schedule",
            Toast.LENGTH_LONG).show()
    }
}