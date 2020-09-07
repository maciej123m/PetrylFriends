package pl.maciej.petrylfriends

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val editTextPreference = preferenceManager.findPreference<EditTextPreference>("nick")
            editTextPreference?.text = MainActivity.mAuth.currentUser!!.displayName.toString()
            editTextPreference?.setOnPreferenceChangeListener { preference, newValue ->
                MainActivity.changeNick(newValue.toString())

            }

            val logOutButton = preferenceManager.findPreference<Preference>("log_out")
            logOutButton!!.setOnPreferenceClickListener {
                MainActivity.mAuth.signOut()
                Toast.makeText(context,getString(R.string.succes_log_out),Toast.LENGTH_SHORT).show()
                requireActivity().finish()
                true
            }
        }



        override fun onResume() {
            super.onResume()
        }

        override fun onPause() {
            super.onPause()
        }

    }


}