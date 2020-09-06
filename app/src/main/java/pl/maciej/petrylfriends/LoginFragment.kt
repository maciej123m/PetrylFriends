package pl.maciej.petrylfriends

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener


class LoginFragment : Fragment() {


    interface onStartListeren{
        fun onStartListener()
    }

    private val listener by lazy {
        context as onStartListeren
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        //sprawdza czy użytkownik jest zalogowany
        if (MainActivity.mAuth.currentUser != null) {
            //jeżeli tak ogarnia UI i przechodzi do main
            updateUI()
            findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
        }
    }

    fun updateDatabase()
    {
        var isBase = true
        MainActivity.database.reference.child("user").
        orderByChild("tokenID").
        equalTo(MainActivity.mAuth.currentUser!!.uid).
        limitToFirst(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.childrenCount==0L)
                    isBase = false
                else
                    return
            }

        })

        while(isBase) {
            Thread.sleep(50)
        }
        //deklaracja tabeli
        val reference = MainActivity.database.reference.child("user").push()

        //tworzenie obiektu user
        val user = UserData(MainActivity.mAuth.currentUser!!.displayName!!, MainActivity.mAuth.currentUser!!.uid,MainActivity.encode(MainActivity.mAuth.currentUser!!.photoUrl.toString()))

        //wstawianie uzytkownika do tablicy
        reference.setValue(user).addOnFailureListener {
                exception ->
            Toast.makeText(context,"${exception.message}", Toast.LENGTH_LONG).show()
        }.addOnCompleteListener {
            Log.d("","")
        }
    }

    //wątek update bazy
    val threadUpdateDatabase : Thread by lazy {
        Thread(Runnable{
            try {
                updateDatabase()
            } catch (e: java.lang.Exception) {
                Log.d(TAG,"błąd przy zapisie do bazy danych")
            }
        })
    }

    fun updateUI()
    {
        MainActivity.userName = MainActivity.mAuth.currentUser!!.displayName.toString()

        Thread(Runnable {
            //pobieranie zdjęcia
            MainActivity.photo =
                MainActivity.generateBitmap(MainActivity.mAuth.currentUser!!.photoUrl.toString())
        }).start()

    }


    //kod logowania
    var RC_SIGN_IN = 123

    fun login()
    {
        //budowanie interfejsu pomiędzy aplikacją a firebase
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        //tworzenie klienta
        val mgoogleSingInClient = GoogleSignIn.getClient(requireActivity(),gso)

        //start aktywności (okienka logowania) wszystko leci do onActivityResult
        val intent = mgoogleSingInClient.signInIntent
        startActivityForResult(intent,RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //jeżeli request się zgadza OK!
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                //logowanie do firebase
                firebaseAuthWithGoogle(account.idToken!!)

            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(context,"Próba logowania nieudana błąd: ${e.statusCode}",Toast.LENGTH_LONG).show()
            }
        }

    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        MainActivity.mAuth.signInWithCredential(credential).addOnCompleteListener {
            Toast.makeText(context,"Próba logowania zakończona pomyślnie",Toast.LENGTH_SHORT).show()
            threadUpdateDatabase.start()
            updateUI()
            //start listenera
            listener.onStartListener()
            //TODO("zrób update")
            findNavController().navigate(R.id.action_loginFragment_to_startFragment)
        }.addOnFailureListener {
                exception ->
                Toast.makeText(context,"Próba logowania zakończona niepowodzeniem kod błędu: ${exception.message}",Toast.LENGTH_SHORT).show()
            }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_login, container, false)

        val button = view.findViewById(R.id.loginGoogleButton) as Button

        button.setOnClickListener {
            login()
        }
        return view
    }

}