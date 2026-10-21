package com.ultrablock.app.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.ultrablock.app.R
import com.ultrablock.app.dns.DomainFilter

class ModulesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modules)

        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val switchBlockSmartTv = findViewById<SwitchMaterial>(R.id.switchBlockSmartTv)
        val switchBlockInAppAds = findViewById<SwitchMaterial>(R.id.switchBlockInAppAds)
        val switchBlockAppTrackers = findViewById<SwitchMaterial>(R.id.switchBlockAppTrackers)
        val switchBlockWps = findViewById<SwitchMaterial>(R.id.switchBlockWps)
        val switchBlockRealme = findViewById<SwitchMaterial>(R.id.switchBlockRealme)
        val switchBlockMeta = findViewById<SwitchMaterial>(R.id.switchBlockMeta)
        val switchBlockXiaomi = findViewById<SwitchMaterial>(R.id.switchBlockXiaomi)
        val switchBlockBytedance = findViewById<SwitchMaterial>(R.id.switchBlockBytedance)

        switchBlockSmartTv.isChecked = DomainFilter.blockSmartTv
        switchBlockInAppAds.isChecked = DomainFilter.blockInAppAds
        switchBlockAppTrackers.isChecked = DomainFilter.blockAppTrackers
        switchBlockWps.isChecked = DomainFilter.blockWps
        switchBlockRealme.isChecked = DomainFilter.blockRealme
        switchBlockMeta.isChecked = DomainFilter.blockMeta
        switchBlockXiaomi.isChecked = DomainFilter.blockXiaomi
        switchBlockBytedance.isChecked = DomainFilter.blockBytedance

        switchBlockSmartTv.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_SMART_TV, isChecked)
        }
        switchBlockInAppAds.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_INAPP_ADS, isChecked)
        }
        switchBlockAppTrackers.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_APP_TRACKERS, isChecked)
        }
        switchBlockWps.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_WPS, isChecked)
        }
        switchBlockRealme.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_REALME, isChecked)
        }
        switchBlockMeta.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_META, isChecked)
        }
        switchBlockXiaomi.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_XIAOMI, isChecked)
        }
        switchBlockBytedance.setOnCheckedChangeListener { _, isChecked ->
            DomainFilter.updateToggle(this, DomainFilter.KEY_BLOCK_BYTEDANCE, isChecked)
        }
    }
}
