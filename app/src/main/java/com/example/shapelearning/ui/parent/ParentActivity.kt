package com.example.shapelearning.ui.parent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.example.shapelearning.databinding.ActivityParentBinding

class ParentActivity : AppCompatActivity() {

    // ViewBinding nesnesi - activity_parent.xml'e karşılık gelir
    private lateinit var binding: ActivityParentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ViewBinding'i kullanarak layout'u inflate et ve contentView'i ayarla
        binding = ActivityParentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        if (savedInstanceState == null) {
            loadParentZoneFragment()
        }
    }

    private fun loadParentZoneFragment() {
        supportFragmentManager.commit {
            replace(binding.parentFragmentContainer.id, ParentZoneFragment())
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}