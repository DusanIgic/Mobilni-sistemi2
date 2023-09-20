package com.example.mosis

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.navigation.fragment.findNavController
import com.example.mosis.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.text.DateFormat.getDateTimeInstance
import java.util.Date

class Register : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var baza : FirebaseFirestore
    private lateinit var pickMedia: ActivityResultLauncher<Intent>
    private var imageUri: Uri? = null
    private var imageUriTemp: Uri? = null
    private lateinit var storage: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        baza = Firebase.firestore
        storage = FirebaseStorage.getInstance(Firebase.app).reference
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        pickMedia = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    if(result.data!=null && result.data!!.data !=null){
                        imageUri = result.data!!.data as Uri
                        binding.registerImage.setImageURI(imageUri)
                    }else{
                        imageUri = imageUriTemp
                        binding.registerImage.setImageURI(imageUriTemp)
                    }
                    Log.d("PhotoPicker", "Selected URI: $imageUri")
                }
                else -> {
                    Log.d("PhotoPicker", "Otkazano")
                }
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val registerButton = binding.btnRegister
        val loginButton = binding.btnLogin
        val email = binding.email
        val password = binding.password
        val username = binding.username
        val phone = binding.phone
        val image = binding.registerImage

        image.setOnClickListener {
            val galleryintent = Intent(Intent.ACTION_GET_CONTENT, null)
            galleryintent.type = "image/*"
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val path = File(requireActivity().filesDir, "Pictures")
            if (!path.exists()) path.mkdirs()
            val imagege = File(path, "Slika_${getDateTimeInstance().format(Date())}.jpg")

            imageUriTemp = FileProvider.getUriForFile(requireActivity(), "com.example.mosis.fileprovider", imagege)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUriTemp)

            val chooser = Intent(Intent.ACTION_CHOOSER)
            chooser.putExtra(Intent.EXTRA_TITLE, "Select from:")
            chooser.putExtra(Intent.EXTRA_INTENT, galleryintent)
            val intentArray = arrayOf(cameraIntent)
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            pickMedia.launch(chooser)
        }

        registerButton.setOnClickListener {
            if (email.text.toString() != "" && password.text.toString() != "" && Patterns.PHONE.matcher(phone.text.toString()).matches() && username.text.toString() != "" && imageUri!=null) {
                auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            val fbImage = storage.child("images/profiles/${auth.currentUser?.uid}")
                            fbImage.putFile(imageUri!!).continueWithTask { task ->
                                if (task.isSuccessful) {
                                    fbImage.downloadUrl
                                } else {
                                    task.exception?.let {
                                        throw it
                                    }
                                }
                            }.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val posts : ArrayList<String> = ArrayList()
                                        val userDb = hashMapOf(
                                            "userId" to auth.currentUser?.uid,
                                            "score" to 0,
                                            "posts" to posts,
                                            "displayName" to  username.text.toString(),
                                            "photoUri" to task.result
                                        )
                                        baza.collection("users").document(auth.currentUser?.uid!!).set(userDb)
                                            .addOnSuccessListener { documentReference ->
                                                Log.d("Doc", "DocumentSnapshot added with ID: $documentReference")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.w("Doc", "Error adding document", e)
                                            }
                                        findNavController().navigate(R.id.action_register_to_map)
                                    }
                                }
                            }
                         else {
                            Log.w("TAG", "signInWithEmail:failure", task.exception)
                            Toast.makeText(this.context, "Dalje neces moci", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        loginButton.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}