package pl.maciej.petrylfriends

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
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

        //avatar użytkownika obcego
        val avatarImageView = holder.view.findViewById<ImageView>(R.id.avatarView)

        //avatar użytkownka
        val myAvatarImageView = holder.view.findViewById<ImageView>(R.id.myAvatarView)

        val box = holder.view.findViewById<LinearLayout>(R.id.mainBox)

        //nick
        val nickText = holder.view.findViewById<TextView>(R.id.nick)

        //tekst główny
        val text = holder.view.findViewById<TextView>(R.id.textViewMain)

        /*//główny pojemnik wiadomości
        val ll = holder.view.findViewById<LinearLayout>(R.id.mainBox)

        //jeżeli mineło dłużej niż 5 minut ma ustawić wartość na true czyli pokazać obrazek
        val boolTime = position != 0 && Messages[position].time - Messages[position-1].time > 300000

        //ustawianie widzialności obrazka, jeżeli poprzednia wiadomość miała tego samego autora
        //lub mineło krócej niż 5 minut i nie jest to pierwsza wiadomość schowaj razem z nickiem
        //inaczej pokaż i nick i obrazek
        if (position != 0 && Messages[position - 1].author == Messages[position].author && !boolTime) {
            holder.view.findViewById<LinearLayout>(R.id.nickAndTimell).visibility = View.GONE
            avatarImageView.visibility = View.GONE
            setMargins(ll,57,0,0,0)

        } else {
            setMargins(ll,5,0,0,0)
            holder.view.findViewById<LinearLayout>(R.id.nickAndTimell).visibility = View.VISIBLE
            avatarImageView.visibility = View.VISIBLE
        }*/

        text.isLongClickable = true //TODO("NIE DZIALA")
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

        nickText.text = Messages[position].author

        text.text = Messages[position].message

        //ustawianie daty
        val formatter = SimpleDateFormat(dataFormat, Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = Messages[position].time
        holder.view.findViewById<TextView>(R.id.data).text = formatter.format(calendar.time)

        if (Messages[position].tokenID == MainActivity.mAuth.currentUser!!.uid) {
            //zamiana obrazków
            myAvatarImageView.visibility = View.VISIBLE
            avatarImageView.visibility = View.GONE

            //zamiana stron boxa
            box.gravity = Gravity.RIGHT
            //zamiana tła
            text.setBackgroundResource(R.drawable.my_item_background)
            //zamiana koloru tekstu
            //text.setTextColor(context.resources.getColor(R.color.my_text_color))

            //chowanie nicku
            nickText.visibility = View.GONE

            myAvatarImageView.setImageBitmap(MainActivity.photo)
        } else {
            //zamiana obrazków
            myAvatarImageView.visibility = View.GONE
            avatarImageView.visibility = View.VISIBLE
            //zamiana stron boxa
            box.gravity = Gravity.LEFT
            //zamiana tła
            text.setBackgroundResource(R.drawable.item_background)
            //zamiana koloru tekstu
            text.setTextColor(context.resources.getColor(R.color.text_color))

            //pokazanie nicku
            nickText.visibility = View.VISIBLE

            for (user in MainActivity.cacheUser) {
                if (Messages[position].tokenID == user.tokenID) {
                    avatarImageView.setImageBitmap(user.avatar)
                    return
                }
            }

            //DALEJ TO PIERWSZE łADOWANIE UżYTKOWNIKA DO CACHE
            FirstLoading(avatarImageView,position)
        }
    }

    //funkcja odpowiadająca za prierzwsze ładowanie
    private fun FirstLoading(avatarImageView : ImageView, position: Int) {
        val inputStream : InputStream = MainActivity.context.assets!!.open("img/er.jpg")
        //tworzenie streama
        val bStream =  BufferedInputStream(inputStream)
        //dekodowanie do bitmapy
        val b = BitmapFactory.decodeStream(bStream)
        avatarImageView.setImageBitmap(b)

        var user : UserData? = null
        MainActivity.database.reference.child("user").
        orderByChild("tokenID").
        equalTo(Messages[position].tokenID).
        limitToFirst(1).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {

                //wczytanie domyślnego avatara
                avatarImageView.setImageBitmap(loadDefaultAvatar())
                //TODO("brak dostępu do bazy, użytkownik zalogowany? - nie możliwe")
            }

            override fun onDataChange(snapshot: DataSnapshot) {

                //jeżeli jest użytkownik którego nie ma w bazie
                if (snapshot.childrenCount == 0L) {
                    avatarImageView.setImageBitmap(loadDefaultAvatar())
                    return
                }

                //wyciągam z dziecka snapshota wartości do user
                for (child in snapshot.children) {
                    user = child.getValue(UserData::class.java)
                }
                user!!.photo = MainActivity.decode(user!!.photo)
                user!!.loadAvatar()
                var i=0
                for (tUser in MainActivity.cacheUser) {
                    if (Messages[position].tokenID == tUser.tokenID) {
                        i++
                    }
                }
                if (i == 0) {
                    MainActivity.cacheUser.add(user!!)
                }

                avatarImageView.setImageBitmap(user!!.avatar)
                notifyItemChanged(position)
            }
        })
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
        val inputStream : InputStream = MainActivity.context.assets!!.open("img/er.jpg")

        //tworzenie streama
        val bStream =  BufferedInputStream(inputStream)

        //dekodowanie do bitmapy
        return BitmapFactory.decodeStream(bStream)
    }

    private fun setMargins(
        view: LinearLayout,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val scale: Float = context.resources.displayMetrics.density
        // convert the DP into pixel
        val l = (left * scale + 0.5f).toInt()
        val r = (right * scale + 0.5f).toInt()
        val t = (top * scale + 0.5f).toInt()
        val b = (bottom * scale + 0.5f).toInt()
        params.setMargins(l,t,r,b)
        view.layoutParams = params
        view.requestLayout()

    }
}
