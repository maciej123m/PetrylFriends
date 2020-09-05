package pl.maciej.petrylfriends

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat
import androidx.core.view.get
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import java.io.BufferedInputStream
import java.io.InputStream
import java.lang.Exception

class MainActivity : AppCompatActivity(), LoginFragment.onStartListeren {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messages = ArrayList()
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        //tablica użytkowników
        cacheUser = ArrayList()
        unSendMessage = ArrayList()
        context = this
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
    private val reference : DatabaseReference by lazy {
        database.reference.child("chat")
    }

    override fun onResume() {
        super.onResume()
        if(mAuth.currentUser!=null)
            onStartListener()
    }

    override fun onPause() {
        super.onPause()
        if(mAuth.currentUser!=null)
            reference.removeEventListener(childEventListener)
    }

    private val childEventListener = (object : ChildEventListener {
        override fun onCancelled(error: DatabaseError) {
            TODO("ogarnij jakąś akcje jak jest anulowane")
            Log.d(MainActivity::class.toString(),"e: $error")
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
        }

        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            var pos : Int = 0
            try {
                val item = snapshot.getValue(Message::class.java)!!
                for (message in messages) {
                    if(message.tokenID == item.tokenID && message.time == item.time) {
                        return
                    }
                }

                messages.add(item)
                pos = messages.count()-1
            }
            catch(e : Exception)
            {
                Toast.makeText(applicationContext,"error $e",Toast.LENGTH_SHORT).show()
            }
            recyclerView?.adapter?.notifyItemInserted(pos)


            val lItem = messages.count()-1

            //scrollowanie na ostatni dodany element
            if(lItem==lastItem) {
                recyclerView.smoothScrollToPosition(lItem)
                recyclerView.scheduleLayoutAnimation()
            }
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
        }
    })


    companion object {

        //zmienna wskazująca na ostatni item w tablicy
        var lastItem = 0

        lateinit var messages : ArrayList<Message>

        //nazwa obecnego użytkownika
        lateinit var userName : String

        //zdjęcie obecnego użytkownika
        lateinit var photo : Bitmap

        //chache użytkowników aby ich danych nie pobierac w kółko
        lateinit var cacheUser : ArrayList<UserData>

        //lista niewysłanych wiadomości (Widok, Pozycja)
        lateinit var unSendMessage: ArrayList<Pair<View?,Int>>

        lateinit var context : Context
        //służy do kodowania url obrazka aby mógł wejść do firebase unikając znaków które nie mogą wejść
        fun encode(data: String) : String {
            var output : String = data.replace(".","1x2")
            output = output.replace("/","2x2")
            output = output.replace("#","3x2")
            return output
        }

        //służy do rozkodowania url obrazka
        fun decode(data: String) : String {
            var output : String = data.replace("1x2",".")
            output = output.replace("2x2","/")
            output = output.replace("3x2","#")
            return output
        }

        val mAuth by lazy {
            FirebaseAuth.getInstance()
        }

        val database : FirebaseDatabase by lazy {
            FirebaseDatabase.getInstance()
        }




        fun generateBitmap(url : String): Bitmap {
            var bitmap : Bitmap? = null

            bitmap = try {
                Picasso.get().load(url).resize(65,65).get()
            } catch (e: Exception) {
                //pobieranie zdjęcia
                val inputStream : InputStream = context.assets!!.open("img/error.jpg")

                //tworzenie streama
                val bStream =  BufferedInputStream(inputStream);

                //dekodowanie do bitmapy
                BitmapFactory.decodeStream(bStream);
            }

            return bitmap!!
        }
    }

    override fun onStartListener() {
        reference.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                TODO("ogarnij jakąś akcje jak jest anulowane")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if (MainActivity.messages.count() != 0) {
                    reference.addChildEventListener(childEventListener)
                    return
                }

                for (item in snapshot.children) {
                    messages.add(item.getValue(Message::class.java)!!)
                }
                recyclerView.adapter!!.notifyDataSetChanged()
                reference.removeEventListener(this)
                reference.addChildEventListener(childEventListener)
            }

        })
    }
}
