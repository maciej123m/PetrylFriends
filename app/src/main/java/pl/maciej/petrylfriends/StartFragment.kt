package pl.maciej.petrylfriends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

/**
 * A simple [Fragment] subclass.
 */
class StartFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_start, container, false)

        //pole edycji nicku
        val textNick = view.findViewById(R.id.edit_text_user_nick) as EditText

        //ustaw nick w edittext
        textNick.setText(MainActivity.mAuth.currentUser!!.displayName)

        //stary nick
        val oldNick = textNick.text.toString()

        //przycisk do ustawiania
        val setNickButton = view.findViewById(R.id.set_nick_button) as Button

        val notNowButton = view.findViewById(R.id.not_now_button) as Button

        setNickButton.setOnClickListener {

            val text = textNick.text.toString().trim()
            if (text.isEmpty() &&
                text.contains('#') &&
                text.contains('.') &&
                text.contains('[') &&
                text.contains(']') &&
                text.contains('$') &&
                text.contains('/')
            ) {
                Toast.makeText(context,"Nick zawiera znaki których nie można używać (#.[]%/)",Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            MainActivity.database.reference.child("user").orderByChild("nick")
                .equalTo(MainActivity.mAuth.currentUser!!.displayName).addListenerForSingleValueEvent(object: ValueEventListener{
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
            val builder = UserProfileChangeRequest.Builder().setDisplayName(textNick.text.toString()).build()

            MainActivity.mAuth.currentUser!!.updateProfile(builder).addOnCompleteListener {
                task ->
                //jeżeli update
                val newNick = MainActivity.mAuth.currentUser!!.displayName.toString()
                if (task.isSuccessful) {
                    //ustaw zapytanie
                    MainActivity.database.reference.child("chat").
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

            findNavController().navigate(R.id.action_startFragment_to_mainFragment)
        }

        notNowButton.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_mainFragment)
        }
        return view
    }

}
