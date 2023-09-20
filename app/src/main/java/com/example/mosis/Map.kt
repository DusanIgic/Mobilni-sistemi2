package com.example.mosis

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.mosis.databinding.FragmentMapBinding
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class Map : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    lateinit var mapa : MapView
    private  var lokacija: Location?=null
    private lateinit var baza: FirebaseFirestore
    private val filterautor = ""

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
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        mapa = binding.mapa

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx: Context? = requireActivity().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx!!))
        if(ActivityCompat.checkSelfPermission(requireActivity(),android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireActivity(),android.Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            registerForActivityResult(ActivityResultContracts.RequestPermission()){
                    isGranted:Boolean->
                if(isGranted){
                    setMyLocationOverlay()
                    LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                        lokacija = it
                        mapa.controller.setZoom(15.0)
                        mapa.controller.setCenter(GeoPoint(lokacija!!.latitude, lokacija!!.longitude))
                    }
                }
            }.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }else {
            setMyLocationOverlay()
            LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                lokacija = it
                mapa.controller.setZoom(15.0)
                mapa.controller.setCenter(GeoPoint(lokacija!!.latitude, lokacija!!.longitude))

            }
        }
            binding.addObject.setOnClickListener {
                findNavController().navigate(R.id.action_map_to_addObject)
            }
            val bundle = arguments
            if (bundle != null) {
                val args = MapArgs.fromBundle(bundle)
                binding.btnLeaderboard.setOnClickListener() {
                    findNavController().navigate(
                        MapDirections.actionMapToLeaderboard(
                            args.filaut,
                            args.razdfil
                        )
                    )
                }
            }
            else{
                binding.btnLeaderboard.setOnClickListener(){
                    findNavController().navigate(R.id.action_map_to_leaderboard)
                }
            }

            binding.btnLogout.setOnClickListener() {
                Firebase.auth.signOut()
                findNavController().navigate(R.id.action_map_to_login)
            }
            binding.btnMapFilter.setOnClickListener() {
                findNavController().navigate(R.id.action_map_to_filter)
            }


        baza.collection("posts").get().addOnSuccessListener {
            for(doc in it!!){
                val marker = Marker(mapa)
                val loc = doc.data!!["location"] as  com.google.firebase.firestore.GeoPoint
                marker.position = GeoPoint(loc!!.latitude, loc!!.longitude)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = doc.data!!["desc"] as String
                marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.baseline_delete_24, null)
                marker.infoWindow = object: MarkerInfoWindow(R.layout.marker, mapa){
                    init{
                        var da = false
                        baza.collection("users").document(auth.currentUser?.uid!!).get().addOnSuccessListener {

                            val idem = it.data!!["posts"] as ArrayList<String>
                            for(c in idem) {
                                Log.d("kako", "1")
                                if(c == doc.id) {
                                    da = true
                                }
                            }
                        }
                        val button = mView.findViewById<Button>(R.id.btn_idem)
                        if(da){
                            button.setHint("Idem")
                        }
                        else{
                            button.setHint("Ne idem")
                        }

                        button.setOnClickListener() {
                            val dist1 = FloatArray(1)
                            Location.distanceBetween(
                                lokacija!!.latitude,
                                lokacija!!.longitude,
                                marker.position.latitude,
                                marker.position.longitude,
                                dist1
                            )
                            if (dist1[0] < 300) {

                                if (da) {
                                    baza.collection("users").document(auth.currentUser?.uid!!).get()
                                        .addOnSuccessListener {
                                            val svi = it.data!!["posts"] as ArrayList<String>
                                            svi.remove(doc.id)

                                            baza.collection("users")
                                                .document(auth.currentUser?.uid!!)
                                                .update("posts", (svi))
                                            baza.collection("users")
                                                .document(auth.currentUser?.uid!!)
                                                .update("score", (it.data!!["score"] as Long) - 2L)
                                        }
                                    button.setHint("Ne idem")
                                    da = !da
                                } else {
                                    baza.collection("users").document(auth.currentUser?.uid!!).get()
                                        .addOnSuccessListener {
                                            baza.collection("users")
                                                .document(auth.currentUser?.uid!!).update(
                                                    "posts",
                                                    (it.data!!["posts"] as ArrayList<String>) + doc.id
                                                )
                                            baza.collection("users")
                                                .document(auth.currentUser?.uid!!)
                                                .update("score", (it.data!!["score"] as Long) + 2L)
                                        }
                                    button.setHint("Idem")
                                    da = !da
                                }
                            }
                        }
                        }

                    }

                val bundle = arguments
                if (bundle != null) {
                    val args = MapArgs.fromBundle(bundle)

                        val dist = FloatArray(1)
                        Location.distanceBetween(
                            lokacija!!.latitude,
                            lokacija!!.longitude,
                            marker.position.latitude,
                            marker.position.longitude,
                            dist
                        )
                        if ((args.razdfil == 0 || dist[0] / 1000 < args.razdfil) && (args.filaut.equals("qazwsx") || marker.title.contains(
                                args.filaut
                            ))){
                            mapa.overlays.add(marker)
                        }

                }
                else{
                    mapa.overlays.add(marker)
                }
            }
        }

    }



    private fun setMyLocationOverlay(){
        val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireActivity()),mapa)
        myLocationOverlay.enableMyLocation()
        mapa.overlays.add(myLocationOverlay)
    }

    override fun onPause() {
        super.onPause()
        mapa.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapa.onResume()
    }
}