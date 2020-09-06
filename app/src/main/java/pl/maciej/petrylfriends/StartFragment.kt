package pl.maciej.petrylfriends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

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

            //funkcja zmiany nicku!
            val success = MainActivity.changeNick(textNick.text.toString())

            if(success)
                findNavController().navigate(R.id.action_startFragment_to_mainFragment)
        }

        //przycisk nie teraz
        notNowButton.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_mainFragment)
        }
        return view
    }

}
