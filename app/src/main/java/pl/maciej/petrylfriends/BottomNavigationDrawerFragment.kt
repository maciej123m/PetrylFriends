package pl.maciej.petrylfriends

import android.R.attr.label
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.bottom_menu_layout.*
import kotlinx.android.synthetic.main.fragment_main.*


class BottomNavigationDrawerFragment : BottomSheetDialogFragment(),
    NavigationView.OnNavigationItemSelectedListener {

    var position : Int? = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    )  = inflater.inflate(R.layout.bottom_menu_layout,container,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigation_view.setNavigationItemSelectedListener(this)
        position = requireArguments().getInt("position")
        val isMyMessage : Boolean? = requireArguments().getBoolean("my")
        if (isMyMessage != null && !isMyMessage) {
            val menu = navigation_view.menu
            menu.findItem(R.id.deleteItem).isVisible = false
            menu.findItem(R.id.editItem).isVisible = false
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when(id)
        {
            R.id.copyItem -> {
                val message by lazy {
                    MainActivity.messages[position!!].message
                }
                val clipboard = requireActivity().getSystemService(MainActivity.CLIPBOARD_SERVICE) as ClipboardManager
                val clip: ClipData = ClipData.newPlainText("Message", message)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, getString(R.string.copied), Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            R.id.deleteItem -> {
                MainActivity.database.reference.child("chat").child(MainActivity.messages[position!!].key.toString()).removeValue().addOnCompleteListener {
                    task ->
                    if (!task.isSuccessful) {
                        Toast.makeText(context,requireContext().getString(R.string.error_delete_message),Toast.LENGTH_SHORT).show()
                    }
                }
                findNavController().popBackStack()
            }
            R.id.editItem -> {
                Toast.makeText(context,"edytuj",Toast.LENGTH_SHORT).show()
            }
            R.id.profileItem ->
                Toast.makeText(context,"edytuj",Toast.LENGTH_SHORT).show()
        }
        return true
    }
}