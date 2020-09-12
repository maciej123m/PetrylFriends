package pl.maciej.petrylfriends

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import java.io.BufferedInputStream
import java.io.InputStream


class MainActivity : AppCompatActivity(), LoginFragment.onStartListeren {

    //zmienna odpowiadająca za sprawdzanie czy klient jest online czy offline
    private val connectedRef by lazy {
        database.getReference(".info/connected")
    }


    private val connectedListener by lazy {
        (object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                TODO("nie ma dostępu do bazy")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    //kiedy jest internet

                    //załadowanie animacji
                    val animation = AnimationUtils.loadAnimation(context,R.anim.slide_to_top)
                    animation.setAnimationListener(object: Animation.AnimationListener{
                        override fun onAnimationRepeat(animation: Animation?) {
                        }

                        override fun onAnimationEnd(animation: Animation?) {
                            //chowa pod koniec animacji cardview
                            cardViewInternet.visibility = View.GONE
                        }

                        override fun onAnimationStart(animation: Animation?) {
                        }

                    })
                    //start animacji
                    cardViewInternet.startAnimation(animation)
                } else {
                    //kiedy nie ma internetu

                    //załadowanie animacji
                    if (ready) {
                        val animation = AnimationUtils.loadAnimation(context,R.anim.slide_from_top)
                        animation.setAnimationListener(object: Animation.AnimationListener{
                            override fun onAnimationRepeat(animation: Animation?) {
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                            }

                            override fun onAnimationStart(animation: Animation?) {
                                //pojawienie się cardview
                                cardViewInternet.visibility = View.VISIBLE
                            }

                        })
                        //start animacji
                        cardViewInternet.startAnimation(animation)
                    }
                }
            }

        })

    }



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        messages = ArrayList()
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        //tablica użytkowników
        cacheUser = ArrayList()
        unSendMessages = ArrayList()
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
         when (item.itemId) {
            R.id.action_settings ->
                startSettings()
            else -> super.onOptionsItemSelected(item)
        }
        return false
    }

    fun startSettings() {
        if (mAuth.currentUser != null) {
            this.startActivity(Intent(this, SettingsActivity::class.java))
        } else {
            Toast.makeText(this,getString(R.string.log_in_info),Toast.LENGTH_SHORT).show()
        }
    }


    private val reference : DatabaseReference by lazy {
        database.reference.child("chat")
    }

    override fun onResume() {
        super.onResume()
        if (mAuth.currentUser != null) {
            onStartListener()
            //odpowiada za to czy użytkownik jest offline czy online
            connectedRef.addValueEventListener(connectedListener)
        }
    }

    override fun onPause() {
        super.onPause()
        if (mAuth.currentUser != null) {
            reference.removeEventListener(childEventListener)
            connectedRef.removeEventListener(connectedListener)
        }

    }

    private val childEventListener = (object : ChildEventListener {
        override fun onCancelled(error: DatabaseError) {
            //TODO("ogarnij jakąś akcje jak jest anulowane")
            Log.d(MainActivity::class.toString(),"e: $error")
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            for((i, item) in messages.withIndex())
            {
                if(item.key == snapshot.key) {
                    val newItem = snapshot.getValue(Message::class.java)!!
                    newItem.key = snapshot.key
                    messages[i] = newItem
                    recyclerView.adapter?.notifyItemChanged(i)
                    return
                }
            }
            Log.d("ss","ss")
        }

        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            var pos  = 0
            try {
                val item = snapshot.getValue(Message::class.java)!!
                item.key = snapshot.key!!
                for (message in messages) {
                    if(message.tokenID == item.tokenID && message.time == item.time) {
                        if (message.key == null)
                            message.key = item.key
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
                recyclerView?.smoothScrollToPosition(lItem)
                recyclerView?.scheduleLayoutAnimation()
            }
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            for((i, item) in messages.withIndex())
            {
                if(item.key == snapshot.key) {
                    messages.removeAt(i)
                    recyclerView.adapter?.notifyItemRemoved(i)
                    return
                }
            }
            Log.d("ss","ss")
        }
    })


    companion object {

        //czy program jest gotowy
        var ready = false

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
        lateinit var unSendMessages: ArrayList<Pair<View?,Int>>

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

        //statyczny konstruktor
        init {
            //database.setPersistenceEnabled(true)
        }


        fun generateBitmap(url : String): Bitmap {
            val bitmap : Bitmap?

            bitmap = try {
                Picasso.get().load(url).resize(65,65).get()
            } catch (e: Exception) {
                //pobieranie zdjęcia
                val inputStream : InputStream = context.assets!!.open("img/error.jpg")

                //tworzenie streama
                val bStream =  BufferedInputStream(inputStream)

                //dekodowanie do bitmapy
                BitmapFactory.decodeStream(bStream)
            }

            return bitmap!!
        }

        fun convertPixelsToDp(
            px: Float,
            context: Context
        ): Float {
            return px / (context.resources
                .displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }

        //funkcja do zmiany nicku w google i w bazie
        fun changeNick(newNick: String) : Boolean {

            val text = newNick.trim()
            if (text.isEmpty() ||
                text.contains('#') ||
                text.contains('.') ||
                text.contains('[') ||
                text.contains(']') ||
                text.contains('$') ||
                text.contains('/')
            ) {
                Toast.makeText(context,"Nick zawiera znaki których nie można używać (#.[]%/)",Toast.LENGTH_LONG).show()
                return false
            }

            val oldNick = mAuth.currentUser!!.displayName

            database.reference.child("user").orderByChild("nick")
                .equalTo(mAuth.currentUser!!.displayName).addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onCancelled(error: DatabaseError) {
                        TODO("Dodaj błąd")
                    }
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.childrenCount != 0L) {
                            //1 element
                            for (item in snapshot.children) {
                                item.ref.child("nick").setValue(text)
                            }
                        }
                    }

                })

            //builder aktualizacji
            val builder = UserProfileChangeRequest.Builder().setDisplayName(newNick).build()

            mAuth.currentUser!!.updateProfile(builder).addOnCompleteListener {
                    task ->
                //jeżeli update
                if (task.isSuccessful) {
                    //ustaw zapytanie
                    database.reference.child("chat").
                    orderByChild("author").
                    equalTo(oldNick).addValueEventListener(object : ValueEventListener{
                        override fun onCancelled(error: DatabaseError) {
                            TODO("dodaj błąd")
                        }

                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (item in snapshot.children) {
                                item.ref.child("author").setValue(newNick)
                            }
                        }

                    })
                }
            }
            return true
        }

    }

    override fun onStartListener() {
        reference.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                TODO("ogarnij jakąś akcje jak jest anulowane")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if (messages.count() != 0) {
                    reference.addChildEventListener(childEventListener)
                    return
                }

                for (item in snapshot.children) {
                    messages.add(item.getValue(Message::class.java)!!)
                }
                recyclerView?.adapter?.notifyDataSetChanged()
                reference.removeEventListener(this)
                reference.addChildEventListener(childEventListener)
            }

        })
    }
}
