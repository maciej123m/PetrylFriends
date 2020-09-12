package pl.maciej.petrylfriends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.bottom_menu_layout.*


class BottomNavigationDrawerFragment : BottomSheetDialogFragment(),
    NavigationView.OnNavigationItemSelectedListener {

    lateinit var standardBottomSheetBehavior : BottomSheetBehavior<View>
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    )  = inflater.inflate(R.layout.bottom_menu_layout,container,false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigation_view.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when(id)
        {
            R.id.copyItem ->
                Toast.makeText(context,"kopiuj",Toast.LENGTH_SHORT).show()
            R.id.deleteItem ->
                Toast.makeText(context,"usun",Toast.LENGTH_SHORT).show()
            R.id.editItem ->
                Toast.makeText(context,"edytuj",Toast.LENGTH_SHORT).show()
        }
        return true
    }
}