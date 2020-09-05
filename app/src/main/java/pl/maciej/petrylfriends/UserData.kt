package pl.maciej.petrylfriends

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.io.BufferedInputStream
import java.io.InputStream

class UserData(val nick : String,val tokenID : String, var photo : String) {
    var avatar : Bitmap? = null

    constructor() :this("","","")


    fun loadAvatar() {
        Thread(Runnable {
            avatar = MainActivity.generateBitmap(photo)
        }).start()
    }
}