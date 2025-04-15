package com.example.shapelearning.ui.parent

import android.os.Bundle
import android.util.Log // Loglama için import
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit // Fragment işlemleri için gerekli import
import com.example.shapelearning.R // R sınıfı importu
import com.example.shapelearning.databinding.ActivityParentBinding

class ParentActivity : AppCompatActivity() {

    // ViewBinding nesnesi - activity_parent.xml'e karşılık gelir
    private lateinit var binding: ActivityParentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ViewBinding'i kullanarak layout'u inflate et ve contentView'i ayarla
        binding = ActivityParentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar'ı ayarla (activity_parent.xml'deki ID'nin 'toolbar' olduğundan emin olun)
        setSupportActionBar(binding.toolbar) // binding.toolbar doğru ID'ye işaret etmeli
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Geri butonunu göster
        supportActionBar?.setDisplayShowHomeEnabled(true) // Geri butonunu göster (bazı durumlarda bu da gerekebilir)

        // Activity ilk kez oluşturuluyorsa ParentZoneFragment'ı yükle
        if (savedInstanceState == null) {
            loadParentZoneFragment()
        }
    }

    private fun loadParentZoneFragment() {
        try {
            // ParentZoneFragment sınıfının doğru pakette olduğundan emin olun
            // FragmentContainerView ID'sinin 'parent_fragment_container' olduğundan emin olun
            supportFragmentManager.commit {
                // R.id.parent_fragment_container doğru ID'ye işaret etmeli
                replace(R.id.parent_fragment_container, ParentZoneFragment::class.java, null)
                // addToBackStack(null) // Genellikle ParentActivity içindeki ilk fragment için backstack'e eklenmez
            }
            Log.d("ParentActivity", "ParentZoneFragment başarıyla yüklendi.")
        } catch (e: Exception) { // Daha genel bir Exception yakalama
            // Hatanın nedenini logla (Fragment bulunamadı mı, başka bir sorun mu?)
            Log.e("ParentActivity", "ParentZoneFragment yüklenirken hata oluştu", e)
            // Kullanıcıya bilgi vermek için bir Toast veya Snackbar gösterilebilir
            // Toast.makeText(this, "Ebeveyn bölümü yüklenemedi.", Toast.LENGTH_SHORT).show()
        }
    }

    // Toolbar'daki geri butonuna basıldığında Activity'yi kapat
    override fun onSupportNavigateUp(): Boolean {
        // Geri tuşuna basıldığında aktiviteyi sonlandır
        // Eğer fragment backstack yönetimi yapıyorsanız, önce onu kontrol edebilirsiniz:
        // if (!supportFragmentManager.popBackStackImmediate()) {
        //     finish()
        // }
        finish()
        return true
    }
}