package com.syed.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.syed.databinding.FragmentContactBinding
import com.syed.models.ContactMessage
import com.syed.models.OfficeDetails
import com.syed.utils.FirebaseUtils
import com.syed.utils.NotificationUtils

class ContactFragment : Fragment() {
    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadUserInfo()
        loadOfficeDetails()
    }

    private fun setupClickListeners() {
        binding.btnSendMessage.setOnClickListener {
            sendMessage()
        }

        binding.btnClearMessage.setOnClickListener {
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

    private fun loadOfficeDetails() {
        FirebaseUtils.firestore
            .collection(FirebaseUtils.OFFICE_DETAILS_COLLECTION)
            .whereEqualTo("active", true)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val office = documents.first().toObject(OfficeDetails::class.java)
                    displayOfficeDetails(office)
                }
            }
    }

    private fun displayOfficeDetails(office: OfficeDetails) {
        binding.tvOfficeName.text = office.name
        binding.tvOfficeAddress.text = office.address
        binding.tvOfficePhone.text = office.phone
        binding.tvOfficeEmail.text = office.email
        binding.tvOfficeHours.text = office.workingHours
    }

    private fun sendMessage() {
        val name =
            binding.etName.text
                .toString()
                .trim()
        val email =
            binding.etEmail.text
                .toString()
                .trim()
        val subject =
            binding.etSubject.text
                .toString()
                .trim()
        val messageText =
            binding.etMessage.text
                .toString()
                .trim()

        // Validation
        if (name.isEmpty() || email.isEmpty() || subject.isEmpty() || messageText.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseUtils.auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSendMessage.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val contactMessage =
            ContactMessage(
                userId = currentUser.uid,
                userName = name,
                userEmail = email,
                subject = subject,
                message = messageText,
            )

        FirebaseUtils.firestore
            .collection(FirebaseUtils.CONTACT_MESSAGES_COLLECTION)
            .add(contactMessage)
            .addOnSuccessListener { documentReference ->
                val messageWithId = contactMessage.copy(id = documentReference.id)
                documentReference.set(messageWithId)

                binding.progressBar.visibility = View.GONE
                binding.btnSendMessage.isEnabled = true

                Toast.makeText(requireContext(), "Message sent successfully!", Toast.LENGTH_LONG).show()
                NotificationUtils.showNotification(
                    requireContext(),
                    "Message Sent",
                    "Your message has been sent. We'll get back to you soon.",
                )

                clearForm()
            }.addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.btnSendMessage.isEnabled = true
                Toast.makeText(requireContext(), "Failed to send message: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun clearForm() {
        binding.etSubject.text?.clear()
        binding.etMessage.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
