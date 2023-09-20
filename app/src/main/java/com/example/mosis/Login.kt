package com.example.mosis

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import androidx.navigation.fragment.findNavController
import com.example.mosis.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class Login : Fragment() {


    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onStart() {
        super.onStart()

        val loginButton = binding.btnLogin
        val signupButton = binding.btnRegister
        val email = binding.email
        val password = binding.password

        if(auth.currentUser != null){
            Log.d("nemoguce", auth.currentUser!!.email.toString())
            findNavController().navigate(R.id.action_login_to_map)
        }

        loginButton.setOnClickListener {
            if (email.text.toString() != "" && password.text.toString() != ""){
                auth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {

                            findNavController().navigate(R.id.action_login_to_map)

                        } else {
                            Log.w("TAG", "signInWithEmail:failure", task.exception)
                            Toast.makeText(this.context, "nesto ne valja", Toast.LENGTH_LONG).show()
                        }
                    }
            }

        }
        signupButton.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}