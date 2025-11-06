package com.syed.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.syed.fragments.MyAdoptionRequestsFragment
import com.syed.fragments.MyRescueRequestsFragment

class MyRequestsPagerAdapter(
    fragmentActivity: FragmentActivity,
) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
        when (position) {
            0 -> MyAdoptionRequestsFragment()
            1 -> MyRescueRequestsFragment()
            else -> MyAdoptionRequestsFragment()
        }
}
