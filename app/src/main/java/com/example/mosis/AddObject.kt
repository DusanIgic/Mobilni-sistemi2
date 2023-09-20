package com.example.mosis

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.example.mosis.databinding.FragmentAddObjectBinding
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.firestore.GeoPoint
import java.io.File
import java.text.DateFormat
import java.util.Date


class AddObject : Fragment() {

    private var _binding: FragmentAddObjectBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var baza : FirebaseFirestore
    private lateinit var storage: StorageReference
    private lateinit var pickMedia: ActivityResultLauncher<Intent>
    private var imageUri: Uri? = null
    private var imageUriTemp: Uri? = null
    private var location: MutableLiveData<Location?> = MutableLiveData(null)
    private var postovi : List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        baza = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        Log.d("wtf", "wtf")

        _binding = FragmentAddObjectBinding.inflate(inflater, container, false)
        storage = FirebaseStorage.getInstance(Firebase.app).reference

        pickMedia = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    if(result.data!=null && result.data!!.data !=null){
                        imageUri = result.data!!.data as Uri
                        binding.objectImage.setImageURI(imageUri)
                    }else{
                        imageUri = imageUriTemp
                        binding.objectImage.setImageURI(imageUriTemp)
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireActivity(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                        location.value = it
                    }
                }
            }.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                location.value = it
            }
        }

        binding.objectImage.setOnClickListener {
            val galleryintent = Intent(Intent.ACTION_GET_CONTENT, null)
            galleryintent.type = "image/*"
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val path = File(requireActivity().filesDir, "Pictures")
            if (!path.exists()) path.mkdirs()
            val imagege = File(path, "Slika_${DateFormat.getDateTimeInstance().format(Date())}.jpg")

            imageUriTemp = FileProvider.getUriForFile(requireActivity(), "com.example.mosis.fileprovider", imagege)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUriTemp)

            val chooser = Intent(Intent.ACTION_CHOOSER)
            chooser.putExtra(Intent.EXTRA_TITLE, "Select from:")
            chooser.putExtra(Intent.EXTRA_INTENT, galleryintent)
            val intentArray = arrayOf(cameraIntent)
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            pickMedia.launch(chooser)
        }

        binding.btnObjectEnd.setOnClickListener {
            if (binding.opis.text.toString() != ""  && imageUri!=null && location.value != null){
                    val postId = baza.collection("posts").document().id
                    val firebaseImages = storage.child("images/garbage/${postId}")
                    firebaseImages.putFile(imageUri!!).continueWithTask { task ->
                        if (task.isSuccessful) {
                            firebaseImages.downloadUrl
                        } else {
                            task.exception?.let {
                                throw it
                            }
                        }
                    }.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            baza.collection("posts").document(postId).update("imageUri", task.result)
                        }
                    }
                    val garbageDb = hashMapOf(
                        "location" to GeoPoint(location.value!!.latitude,location.value!!.longitude),
                        "desc" to binding.opis.text.toString(),
                        "owner" to baza.collection("users").document(auth.currentUser?.uid!!),
                    )
                    baza.collection("posts").document(postId).set(garbageDb).addOnSuccessListener { findNavController().navigate(R.id.action_addObject_to_map)
                        baza.collection("users").document(auth.currentUser?.uid!!).get().addOnSuccessListener {
                            baza.collection("users").document(auth.currentUser?.uid!!).update("score", (it.data!!["score"] as Long) + 4L)
                            }
                        }
                    }
                }


        binding.btnObjectBack.setOnClickListener {
            findNavController().navigate(R.id.action_addObject_to_map)
        }
        }
    }


