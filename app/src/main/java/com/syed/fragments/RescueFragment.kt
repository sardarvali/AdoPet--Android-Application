package com.syed.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.syed.databinding.FragmentRescueBinding
import com.syed.models.RescueRequest
import com.syed.utils.FirebaseUtils
import com.syed.utils.NotificationUtils

class RescueFragment : Fragment() {
    private var _binding: FragmentRescueBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRescueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadUserInfo()
    }

    private fun setupClickListeners() {
        binding.btnSubmitRequest.setOnClickListener {
            submitRescueRequest()
        }

        binding.btnClear.setOnClickListener {
            clearForm()
        }
    }

    private fun loadUserInfo() {
        val currentUser = FirebaseUtils.auth.currentUser
        currentUser?.let { user ->
            binding.etName.setText(user.displayName ?: "")
            binding.etEmail.setText(user.email ?: "")
        }
    }

    private fun submitRescueRequest() {
        val name =
            binding.etName.text
                .toString()
                .trim()
        val email =
            binding.etEmail.text
                .toString()
                .trim()
        val phone =
            binding.etPhone.text
                .toString()
                .trim()
        val location =
            binding.etLocation.text
                .toString()
                .trim()
        val description =
            binding.etDescription.text
                .toString()
                .trim()
        val petType = binding.spinnerPetType.selectedItem.toString()
        val urgency =
            binding.spinnerUrgency.selectedItem
                .toString()
                .lowercase()

        // Validation
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() ||
            location.isEmpty() || description.isEmpty()
        ) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseUtils.auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmitRequest.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val rescueRequest =
            RescueRequest(
                userId = currentUser.uid,
                userName = name,
                userEmail = email,
                userPhone = phone,
                location = location,
                description = description,
                petType = petType.lowercase(),
                urgency = urgency,
            )

        FirebaseUtils.firestore
            .collection(FirebaseUtils.RESCUE_REQUESTS_COLLECTION)
            .add(rescueRequest)
            .addOnSuccessListener { documentReference ->
                val requestWithId = rescueRequest.copy(id = documentReference.id)
                documentReference.set(requestWithId)

                binding.progressBar.visibility = View.GONE
                binding.btnSubmitRequest.isEnabled = true

                Toast.makeText(requireContext(), "Rescue request submitted successfully!", Toast.LENGTH_LONG).show()
                NotificationUtils.showNotification(
                    requireContext(),
                    "Rescue Request Submitted",
                    "Your rescue request has been submitted. We'll contact you soon.",
                )

                clearForm()
            }.addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.btnSubmitRequest.isEnabled = true
                Toast.makeText(requireContext(), "Failed to submit request: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun clearForm() {
        binding.etPhone.text?.clear()
        binding.etLocation.text?.clear()
        binding.etDescription.text?.clear()
        binding.spinnerPetType.setSelection(0)
        binding.spinnerUrgency.setSelection(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
