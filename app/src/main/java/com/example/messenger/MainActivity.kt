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
import kotlinx.android.synthetic.main.content_main.*
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
    val ID = arrayListOf<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if(User.login == "")
        {
            val intent = Intent(this, Settings::class.java)
            startActivity(intent)
        }

        listView = findViewById(R.id.listView)
        getMsg()

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        //--------------------------------------------------

        //val list: ArrayList<String> = ArrayList()


        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.INVISIBLE
        /*
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
                }
                listView.adapter = ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1,list)
            }.execute("GET", "http://tgryl.pl/shoutbox/messages")

        } */

        val buttonPost = findViewById<Button>(R.id.buttonPost)
        buttonPost.setOnClickListener() {
            sendMSG()
        }


        listView.setOnItemClickListener { _, _, position, _ ->
            replaceMsg(ID[position])
            getMsg()
            true

        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            removeMsg(ID[position])
            getMsg()
            true
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
            val intent = Intent(this,MainActivity::class.java)
                startActivity(intent)
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

    private fun getMsg(){
        HttpTask {
            if (it == null) {
                println("connection error")
                return@HttpTask
            }
            val msg = arrayListOf<String>()
            ID.clear()
            for (json in JSONArray(it)) {
                println(json)
                msg.add(json.getString("content")+" From: " + json.getString("login"))
                ID.add(json.getString("id"))
            }
            val adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,msg)
            listView.adapter = adapter
        }.execute("GET", "http://tgryl.pl/shoutbox/messages")
    }

    private fun replaceMsg(del: String){
        val json = JSONObject()
        json.put("content", editTextSend.text)
        json.put("login", User.login)

        HttpTask {
            if (it == null) {
                println("connection error")
                return@HttpTask
            }
        }.execute("PUT", "http://tgryl.pl/shoutbox/message/$del", json.toString())
        println("http://tgryl.pl/shoutbox/message/$del")
        println("Login " + User.login)
        println("Content " + editTextSend.text.toString())
    }

    private fun removeMsg(del: String){
        HttpTask {
            if (it == null) {
                println("connection error")
                return@HttpTask
            }
        }.execute("DELETE", "http://tgryl.pl/shoutbox/message/$del")
        println("http://tgryl.pl/shoutbox/message/$del")
    }

    private fun sendMSG(){
        if(editTextSend.text.isNotEmpty()) {
            val json = JSONObject()
            json.put("content", editTextSend.text)
            json.put("login", User.login)


            progressBar.visibility = View.VISIBLE
            HttpTask {
                progressBar.visibility = View.INVISIBLE
                if (it == null) {
                    println("connection error")
                    return@HttpTask
                }
                println(it)
            }.execute("POST", "http://tgryl.pl/shoutbox/message", json.toString())
            getMsg()
            editTextSend.setText("")
        }
    }


    companion object {
        val TAG = "MainActivity"
    }


}
