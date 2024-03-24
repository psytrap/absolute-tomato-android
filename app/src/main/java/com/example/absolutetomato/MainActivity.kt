package com.example.absolutetomato

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.ui.AppBarConfiguration
import com.example.absolutetomato.databinding.ActivityMainBinding
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.floor

/*
Timer().schedule(object : TimerTask() {
    override fun run() {
        // Assuming you're passing the application context to avoid leaks
        val notification = buildNotification(appContext)

        // Notification manager requires the main thread to post notifications
        Handler(Looper.getMainLooper()).post {
            val notificationManager = ContextCompat.getSystemService(appContext, NotificationManager::class.java) as NotificationManager
            notificationManager.notify(notificationId, notification) // notificationId is a unique int for each notification that you must define
        }
    }
}, delay) // delay is the time in milliseconds before the task is executed
*/



class MainActivity : AppCompatActivity() {

    private lateinit var relaxSeekBar: SeekBar
    private lateinit var focusTextView: TextView
    private lateinit var relaxTextView: TextView
    private lateinit var focusSeekBar: SeekBar
    private lateinit var notificationSeekBar: SeekBar
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding



    private var seconds = AtomicInteger(0) // only used within the timer
    private var timer: Timer? = null
    private val uiHandler = Handler(Looper.getMainLooper())
    var timerState = TimerState.OFF
    val channelId = "timer_notification_channel"
    val TIMER_NOTIFICATION_CODE = 777

    enum class TimerState {
        FOCUS {
            override fun performAction() {
                println("Performing action for State FOCUS")
            }
        },
        RELAX {
            override fun performAction() {
                println("Performing action for State RELAX")
            }
        },
        OFF {
            override fun performAction() {
                println("Performing action for State OFF")
            }
        };

        abstract fun performAction()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        focusTextView = binding.contentMain.workTimerTextView
        relaxTextView = binding.contentMain.breakTimerTextView
        focusSeekBar = binding.contentMain.workSeekBar
        relaxSeekBar = binding.contentMain.breakSeekBar
        notificationSeekBar = binding.contentMain.notificationSeekBar

        createNotificationChannel()


        setSupportActionBar(binding.toolbar)

        binding.contentMain.workButton.isEnabled = false
        binding.contentMain.breakButton.isEnabled = false
        binding.contentMain.stopTimer.isEnabled = false

        focusTextView = binding.contentMain.workTimerTextView
        relaxTextView = binding.contentMain.breakTimerTextView
        focusSeekBar = binding.contentMain.workSeekBar
        relaxSeekBar = binding.contentMain.breakSeekBar
        notificationSeekBar = binding.contentMain.notificationSeekBar

        binding.contentMain.startTimer.setOnClickListener {
            binding.contentMain.startTimer.isEnabled = false
            binding.contentMain.stopTimer.isEnabled = true
            binding.contentMain.breakButton.isEnabled = true
            startTimer()
        }
        binding.contentMain.stopTimer.setOnClickListener {
            binding.contentMain.startTimer.isEnabled = true
            binding.contentMain.stopTimer.isEnabled = false
            binding.contentMain.breakButton.isEnabled = false
            binding.contentMain.workButton.isEnabled = false
            stopTimer()
        }
        binding.contentMain.workButton.setOnClickListener {
            binding.contentMain.breakButton.isEnabled = true
            binding.contentMain.workButton.isEnabled = false
            seconds.set(0)
            timerState = TimerState.FOCUS
            updateTimer(relaxTextView, 0, relaxSeekBar.progress)
        }
        binding.contentMain.breakButton.setOnClickListener {
            binding.contentMain.breakButton.isEnabled = false
            binding.contentMain.workButton.isEnabled = true

            seconds.set(0)
            timerState = TimerState.RELAX
        }


        focusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minutes = progress * 5
                binding.contentMain.workTextView.text = "Focus interval: $minutes Minutes"
                updateTimer(focusTextView, 0, focusSeekBar.progress * 5)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
        })
        relaxSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minutes = progress
                binding.contentMain.breakTextView.text = "Relax interval: $minutes Minutes"
                updateTimer(relaxTextView, 0, relaxSeekBar.progress) // TODO timerState
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
        })
        notificationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val seconds = (progress+1) * 10
                binding.contentMain.notificationTextView.text = "Notification interval: $seconds Seconds"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { // empty
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { // empty
            }
        })
        val sharedPreferences = getSharedPreferences("AbsoluteTomatoPrefs", Context.MODE_PRIVATE)

        focusSeekBar.setProgress( sharedPreferences.getInt("focusInterval", 25) / 5, false)
        relaxSeekBar.setProgress( sharedPreferences.getInt("relaxInterval", 5), false)
        notificationSeekBar.setProgress( (sharedPreferences.getInt("notificationInterval", 20) / 10) - 1 , false)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    private fun startTimer() {
        if(timer != null ) {
            return // nothing to do
        }
        seconds.set(0)
        timerState = TimerState.FOCUS
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                uiHandler.post {
                    Log.d("DEBUG", "Timer: $(seconds.get())")
                    var interval = -1
                    if (timerState == TimerState.FOCUS) {
                        interval = focusSeekBar.progress * 5
                        updateTimer(focusTextView, seconds.get(), interval)
                    } else { // TODO exception
                        interval = relaxSeekBar.progress * 1
                        updateTimer(relaxTextView, seconds.get(), interval )
                    }
                    val delta = interval * 60 - seconds.get()
                    val notification_interval = (notificationSeekBar.progress + 1) *10
                    if ( delta <= 0) {
                        if ( delta % notification_interval == 0) {
                            val title = if (timerState == TimerState.FOCUS) {
                                "Focus interval completed"
                            } else {
                                "Relax interval completed"
                            }
                            val text = if (timerState == TimerState.FOCUS) {
                                "Exceeding focus interval by " + composeTimerText(delta)
                            } else {
                                "Exceeding relax interval by " + composeTimerText(delta)
                            }
                            sendTimerNotification(title, text)
                        }
                    } else {
                        // Check and clear notification
                        // TODO
                        clearTimerNotification()
                    }
                    seconds.incrementAndGet()
                }
            }
        }, 0, 1000) // 0 Initial delay, 1000ms period
    }
    private fun stopTimer() {
        updateTimer(focusTextView, 0, focusSeekBar.progress * 5)
        updateTimer(relaxTextView, 0, relaxSeekBar.progress)
        timer?.cancel()
        timer?.purge()
        timer = null
        clearTimerNotification()
    }

    private fun updateTimer(view : android.widget.TextView, timer : Int, interval : Int)  {
        // TODO
        val delta = interval * 60 - timer
        Log.d("DEBUG", "Delta $delta")
        val str = composeTimerText(delta)
        view.text = str
    }
    private fun composeTimerText(delta: Int) : String {
        val minutes = floor(abs(delta.toDouble()) / 60 ).toInt()
        val seconds = (abs(delta.toDouble()) % 60).toInt()
        return (if (delta >= 0)  "" else "+") + minutes.toString().padStart(1, '0') + 'm' + seconds.toString().padStart(2, '0') + 's'

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                fun showDialog(context: Context) {
                    val dialogView = LayoutInflater.from(context).inflate(R.layout.about_dialog, null)
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("About")
                    builder.setView(dialogView)
                    builder.setPositiveButton("OK") { dialog, _ -> // nothing
                        dialog.dismiss()
                    }
                    // Create and show the dialog
                    builder.show()
                }
                showDialog(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Timer expirations"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Notification when focus or relaxation intervals ended"
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendTimerNotification(title: String, text : String) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 666)
            return
        }
        //val intent = Intent(this, MainActivity::class.java) //.apply {
        //      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        //  }
        //val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)


        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.tomato)
            .setContentTitle(title)
            .setContentText(text) // TODO
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        with(NotificationManagerCompat.from(this)) {
            notify(TIMER_NOTIFICATION_CODE, builder.build())
        }
    }

    private fun clearTimerNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(TIMER_NOTIFICATION_CODE) // Replace 'notificationId' with the actual ID of the notification to cancel
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            666 -> { // TODO Magic number
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted, yay! Do the
                    // related task you need to do.
                } else {
                    // Permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save your state here.
        Log.d("DEBUG", "onSafetState")
        //outState.putString("relaxInterval", relaxSeekBar.progress.toString())
    }
    override fun onStop() {
        Log.d("DEBUG", "onStop")
        val sharedPreferences = getSharedPreferences("AbsoluteTomatoPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("focusInterval", focusSeekBar.progress * 5) // Replace "key" with your actual key, and "value" with the value you want to save.
        editor.putInt("relaxInterval", relaxSeekBar.progress) // Replace "key" with your actual key, and "value" with the value you want to save.
        editor.putInt("notificationInterval", (notificationSeekBar.progress +1 ) * 10) // Replace "key" with your actual key, and "value" with the value you want to save.
        editor.apply() // or editor.commit() for synchronous saving.
        super.onStop()
    }


    override fun onDestroy() {
        // TODO say goodbye in case timer is not null
        stopTimer()
        Log.d("DEBUG", "onDestroy")
        super.onDestroy()
    }


}