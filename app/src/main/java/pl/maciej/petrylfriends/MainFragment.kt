package pl.maciej.petrylfriends

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*


class MainFragment : Fragment() {

    private lateinit var editText : EditText
    private lateinit var sendButton : ImageButton
    private lateinit var scrollListener : RecyclerView.OnScrollListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        val recyclerView = view.findViewById(R.id.recyclerView) as RecyclerView
        recyclerView.setHasFixedSize(true)

        recyclerView.adapter = RecyclerViewAdapter(requireContext())

        //dodanie linearnego layoutu do wyświetlania wiadomości
        val layoutManger = LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false)

        //ustawianie wysyłania wiadomości od dołu
        layoutManger.stackFromEnd = true

        //ustawianie litenera na scroll czyli szukanie pozycji ostatniego elementu jaki jest wyświetlany
        scrollListener = object :
            RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManger.childCount
                val firstVisibleItemPosition: Int = layoutManger.findFirstVisibleItemPosition()

                //ostatni widziany element na liście
                MainActivity.lastItem = firstVisibleItemPosition + visibleItemCount
            }
        }

        //ustawianie listenera scrolla
        recyclerView.addOnScrollListener(scrollListener)

        //ustawienie mangera layoutu
        recyclerView.layoutManager = layoutManger

        //ustawianie cache dla wiadomości
        //recyclerView.setItemViewCacheSize(30)

        //pole wpisywania tekstu
        editText = view.findViewById(R.id.editText) as EditText
        editText.editText.addTextChangedListener(textWatcher)

        //przycisk wysyłania wiadomości
        sendButton = view.findViewById(R.id.sendButton)

        sendButton.setOnClickListener {
            //wysyłam do głównej aktywności
            val position : Int = onUpdate(editText.text.toString())
            //jeżeli zwróci -1 znaczy że ciąg był pusty
            if(position == -1)
                return@setOnClickListener
            //powiadamia recyclerview o dodaniu itemu
            (recyclerView.adapter as RecyclerViewAdapter).notifyItemInserted(position)
            //ustawia wiadomość jako niewysłana
            editText.text = null

            glueToBottomRecyclerView()
        }

        //przycisk ma być ukryty na początku działania programu
        sendButton.visibility = View.GONE

        return view
    }


    //obiekty interfejsu textWatcher
    lateinit var animator : Animation
    val textWatcher : TextWatcher = (object : TextWatcher{
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            //sprawdzenie czy editText jest pusty
            if(editText.text.trim().isEmpty()) {

                //sprawdzanie czy przycisk jest widoczny
                //jeżeli tak - animacja się wykonuje
                if(sendButton.visibility != View.VISIBLE)
                    return

                //załadowanie animacji do animatora
                animator = AnimationUtils.loadAnimation(activity,R.anim.hide_button)

                //ustawienie listenera na animator
                animator.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationRepeat(animation: Animation?) {
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        //schowanie przycisku po odtworzeniu animacji
                        sendButton.visibility = View.GONE
                    }

                    override fun onAnimationStart(animation: Animation?) {
                    }
                })

                //wyświetlenie animacji
                sendButton.startAnimation(animator)
            }
            else {

                //sprawdzanie czy przycisk jest widoczny
                //jeżeli tak - animacja się nie wykonuje
                if(sendButton.visibility == View.VISIBLE)
                    return
                //załadowanie animacji
                animator = AnimationUtils.loadAnimation(activity, R.anim.show_button)

                //ustawienie listenera
                animator.setAnimationListener(object : Animation.AnimationListener{
                    override fun onAnimationRepeat(animation: Animation?) {
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                    }

                    override fun onAnimationStart(animation: Animation?) {
                        //wyświetlenie przycisku przed wykonaiem animacji aby było ją widać
                        sendButton.visibility = View.VISIBLE
                    }

                })
                //rozpoczęcie animacji
                sendButton.startAnimation(animator)
            }
        }
    })

    override fun onResume() {
        super.onResume()
        if(MainActivity.mAuth.currentUser==null) {
            findNavController().navigate(R.id.action_mainFragment_to_loginFragment)
        }
    }

    override fun onPause() {
        super.onPause()
    }

    private fun glueToBottomRecyclerView()
    {
        //pobieranie ostatniego indexu z tablicy
        val lItem = MainActivity.messages.count()-1

        //scrollowanie na ostatni dodany element
        if(lItem==MainActivity.lastItem) {
            recyclerView.smoothScrollToPosition(lItem)
            recyclerView.scheduleLayoutAnimation()
        }
    }

    fun onUpdate(message: String) : Int {
        //sprawdzanie czy wiadomość nie jest pusta
        if(message.trim().isNotEmpty()) {
            val messageObject = Message(
                MainActivity.userName,message,
                MainActivity.mAuth.currentUser!!.uid,System.currentTimeMillis())
            //dodanie do tablicy wiadomości
            MainActivity.messages.add(messageObject)
            val pos = MainActivity.messages.count()-1

            //dodanie nieysłanej jeszcze wiadomości
            MainActivity.unSendMessages.add(Pair(null,pos))

            //update bazy
            updateDatabase(messageObject, MainActivity.messages.count())

            return pos
        }
        return -1
    }
    
    private fun updateDatabase(message: Message, position : Int) {
        Thread(Runnable {
            val reference = MainActivity.database.reference.child("chat").push()
            reference.setValue(message).addOnCompleteListener {
                    task ->
                if (task.isSuccessful) {
                    //jeżeli wysłano zaznaczenie wiadomości jako wysłana

                    for ((i, item) in MainActivity.unSendMessages.withIndex()) {
                        if (position-1 == item.second) {
                            //do momentu aż view nie przyjdzie ma na niego oczekiwać
                            while(item.first==null) {
                                try {
                                    //usypianie wątką na 50milis
                                    Thread.sleep(50)
                                } catch (e: InterruptedException) {
                                    Log.d("","")
                                }
                            }

                            //podmiana koloru tekstu
                            item.first!!.findViewById<TextView>(R.id.textViewMain).setTextColor(requireContext().getColor(R.color.text_color))

                            //podmień item
                            recyclerView.adapter!!.notifyItemChanged(item.second,item.first)
                             try {
                                //usuń z tablicy
                                MainActivity.unSendMessages.removeAt(i)
                            } catch (e : ConcurrentModificationException) {
                                Log.d("TAG","wystąpił wyjątek")
                            }
                            break
                        }
                    }

                } else {
                    TODO("akcja przy zwróceniu isSuccessful - false")
                }
            }.addOnCanceledListener {
                Log.d("","")
                TODO("akcja przy braku internetu?")
            }
        }).start()
    }


}
