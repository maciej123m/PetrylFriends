package pl.maciej.petrylfriends

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.io.BufferedInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


class RecyclerViewAdapter(val context: Context, val listener: onClickItem) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>(){

    interface onClickItem
    {
        fun onclickMessage(position: Int)
        fun onclickAvatar(TokenID : String)
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    var Messages : ArrayList<Message> = MainActivity.messages

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.item,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
       return Messages.size
    }

    //formatowanie daty
    private val dataFormat = "E HH:mm"

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //za wczasu ustawiam kolor tekstu (błędy w cachowaniu)
        val text = holder.view.findViewById<TextView>(R.id.textViewMain)
        text.setTextColor(context.getColor(R.color.text_color))

        text.isLongClickable = true
        text.setOnLongClickListener {
            listener.onclickMessage(position)
            return@setOnLongClickListener true
        }

        //sprawdza czy lista niewysłanych wiadomości nie jest pusta, czy się zgadza token i czy jest element ten na liście niewysłanych wiadomości
        if (MainActivity.unSendMessages.count() != 0 && Messages[position].tokenID == MainActivity.mAuth.currentUser!!.uid && isOnList(position)) {

            //dodaje view do listy
            //iteruje tablice unSendMessages
            for (i in 0 until MainActivity.unSendMessages.count()) {
                //jeżeli pozycje się zgadzają to dodaje view
                if (MainActivity.unSendMessages[i].second == position) {
                    MainActivity.unSendMessages[i] = Pair<View,Int>(holder.view, position)
                }
            }
            holder.view.findViewById<TextView>(R.id.textViewMain).setTextColor(context.getColor(R.color.un_send_text_color))
        }

        //ustawia gotowość na true
        MainActivity.ready = true

        holder.view.findViewById<TextView>(R.id.nick).text = Messages[position].author

        text.text = Messages[position].message

        //ustawianie daty
        val formatter = SimpleDateFormat(dataFormat, Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = Messages[position].time
        holder.view.findViewById<TextView>(R.id.data).text = formatter.format(calendar.time)

        if (Messages[position].tokenID == MainActivity.mAuth.currentUser!!.uid) {
            holder.view.findViewById<ImageView>(R.id.avatarView).setImageBitmap(MainActivity.photo)
        } else {
            for (user in MainActivity.cacheUser) {
                if (Messages[position].tokenID == user.tokenID) {
                    holder.view.findViewById<ImageView>(R.id.avatarView).setImageBitmap(user.avatar)
                    return
                }
            }

            val inputStream : InputStream = MainActivity.context.assets!!.open("img/error.jpg")
            //tworzenie streama
            val bStream =  BufferedInputStream(inputStream)
            //dekodowanie do bitmapy
            val b = BitmapFactory.decodeStream(bStream)
            holder.view.findViewById<ImageView>(R.id.avatarView).setImageBitmap(b)

            var user : UserData? = null
            MainActivity.database.reference.child("user").
            orderByChild("tokenID").
            equalTo(Messages[position].tokenID).
            limitToFirst(1).addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onCancelled(error: DatabaseError) {

                    //wczytanie domyślnego avatara
                    holder.view.findViewById<ImageView>(R.id.avatarView).setImageBitmap(loadDefaultAvatar())
                    //TODO("brak dostępu do bazy, użytkownik zalogowany? - nie możliwe")
                }

                override fun onDataChange(snapshot: DataSnapshot) {

                    //jeżeli jest użytkownik którego nie ma w bazie
                    if (snapshot.childrenCount == 0L) {
                        holder.view.findViewById<ImageView>(R.id.avatarView).setImageBitmap(loadDefaultAvatar())
                        return
                    }

                    //wyciągam z dziecka snapshota wartości do user
                    for (child in snapshot.children) {
                        user = child.getValue(UserData::class.java)
                    }
                    user!!.photo = MainActivity.decode(user!!.photo)
                    user!!.loadAvatar()
                    holder.view.findViewById<ImageView>(R.id.avatarView).setImageBitmap(user!!.avatar)
                    notifyItemChanged(position)
                    var i=0
                    for (tUser in MainActivity.cacheUser) {
                        if (Messages[position].tokenID == tUser.tokenID) {
                            i++
                        }
                    }
                    if (i == 0) {
                        MainActivity.cacheUser.add(user!!)
                    }
                }
            })
        }
    }

    fun isOnList(position: Int): Boolean {
        for (item in MainActivity.unSendMessages) {
            if (item.second == position) {
                return true
            }
        }
        return false
    }

    fun loadDefaultAvatar() : Bitmap
    {
        val inputStream : InputStream = MainActivity.context.assets!!.open("img/error.jpg")

        //tworzenie streama
        val bStream =  BufferedInputStream(inputStream)

        //dekodowanie do bitmapy
        return BitmapFactory.decodeStream(bStream)
    }
}
