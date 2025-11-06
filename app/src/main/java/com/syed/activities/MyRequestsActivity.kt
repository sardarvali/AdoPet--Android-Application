package com.syed.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.syed.adapters.MyRequestsPagerAdapter
import com.syed.databinding.ActivityMyRequestsBinding

class MyRequestsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyRequestsBinding
    private lateinit var pagerAdapter: MyRequestsPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewPager()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Requests"
    }

    private fun setupViewPager() {
        pagerAdapter = MyRequestsPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text =
                when (position) {
                    0 -> "Adoption Requests"
                    1 -> "Rescue Requests"
                    else -> "Tab ${position + 1}"
                }
        }.attach()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
