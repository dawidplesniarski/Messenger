package com.example.messenger

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

operator fun JSONArray.iterator(): Iterator<JSONObject> =
    (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

val TIMEOUT = 10*1000

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    lateinit var listView : ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        listView = findViewById(R.id.listView)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        //--------------------------------------------------

        val list: ArrayList<String> = ArrayList()


        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.INVISIBLE

        val button = findViewById<Button>(R.id.buttonGet)
        button.setOnClickListener() {
            progressBar.visibility = View.VISIBLE
            HttpTask {
                progressBar.visibility = View.INVISIBLE
                if (it == null) {
                    println("connection error")
                    return@HttpTask
                }
                for (json in JSONArray(it)) {
                    println(json)
                    list.add(json.toString())
                    //val TV = findViewById<TextView>(R.id.textView)
                    //TV.append(json.toString())
                }
                listView.adapter = ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1,list)
            }.execute("GET", "http://tgryl.pl/shoutbox/messages")

        }

        val buttonPost = findViewById<Button>(R.id.buttonPost)
        buttonPost.setOnClickListener() {

            val json = JSONObject()
            json.put("content", "CONTENT")
            json.put("login", "LOGIN")
            json.put("date", "DATE")
            json.put("id", "ID")
            json.put("done", false)

            progressBar.visibility = View.VISIBLE
            HttpTask {
                progressBar.visibility = View.INVISIBLE
                if (it == null) {
                    println("connection error")
                    return@HttpTask
                }
                println(it)
            }.execute("POST", " http://tgryl.pl/shoutbox/messages", json.toString())
        }

        //--------------------------------------------------


    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_shoutbox -> {

            }
            R.id.nav_settings -> {
                val intent = Intent(this, Settings::class.java)
                startActivity(intent)
            }

        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    class HttpTask(var callback: (String?) -> Unit) : AsyncTask<String, Unit, String>()  {

        override fun doInBackground(vararg params: String): String? {
            val url = URL(params[1])
            val httpClient = url.openConnection() as HttpURLConnection
            httpClient.setReadTimeout(TIMEOUT)
            httpClient.setConnectTimeout(TIMEOUT)
            httpClient.requestMethod = params[0]

            if (params[0] == "POST") {
                httpClient.instanceFollowRedirects = false
                httpClient.doOutput = true
                httpClient.doInput = true
                httpClient.useCaches = false
                httpClient.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            try {
                if (params[0] == "POST") {
                    httpClient.connect()
                    val os = httpClient.getOutputStream()
                    val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                    writer.write(params[2])
                    writer.flush()
                    writer.close()
                    os.close()
                }
                if (httpClient.responseCode == HttpURLConnection.HTTP_OK) {
                    val stream = BufferedInputStream(httpClient.inputStream)
                    val data: String = readStream(inputStream = stream)
                    return data
                } else {
                    println("ERROR ${httpClient.responseCode}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                httpClient.disconnect()
            }

            return null
        }

        fun readStream(inputStream: BufferedInputStream): String {
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            bufferedReader.forEachLine { stringBuilder.append(it) }
            return stringBuilder.toString()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            callback(result)
        }
    }

    companion object {
        val TAG = "MainActivity"
    }


}
