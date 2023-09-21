package com.example.mosis

import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.mosis.databinding.FragmentLeaderboardBinding
import com.example.mosis.databinding.FragmentLoginBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class Leaderboard : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var baza: FirebaseFirestore
    private var users: List<DocumentSnapshot>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        baza = Firebase.firestore
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val bundle = arguments
        if (bundle != null) {
            val args = LeaderboardArgs.fromBundle(bundle)
            binding.btnBackleaderboard.setOnClickListener() {
                findNavController().navigate(LeaderboardDirections.actionLeaderboardToMap(args.filraz,args.filaut))
            }
        }
        else{
            binding.btnBackleaderboard.setOnClickListener() {
                findNavController().navigate(R.id.action_leaderboard_to_map)
            }
        }

        baza.collection("users").get().addOnSuccessListener {

            users = it.documents
            val adapter = object :
                ArrayAdapter<DocumentSnapshot>(requireContext(), R.layout.list_row, users!!) {
                var items: List<DocumentSnapshot>

                init {
                    items = users!!.sortedBy {
                        it.data!!["score"] as Long
                    }.reversed()
                }

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    var listItem = convertView
                    if (listItem == null) {
                        listItem =
                            LayoutInflater.from(context).inflate(R.layout.list_row, parent, false)
                    }
                    listItem?.findViewById<ImageView>(R.id.profil)?.load(Uri.parse(items[position].data!!["photoUri"] as String))
                    listItem?.findViewById<TextView>(R.id.name_leaderboard)?.text =
                        items[position].data!!["displayName"] as String
                    listItem?.findViewById<TextView>(R.id.leaderboard_score)?.text =
                        (items[position].data!!["score"] as Long).toString()
                    return listItem!!
                }
            }
            requireActivity().findViewById<ListView>(R.id.mobile_list).adapter = adapter
        }
    }
}
