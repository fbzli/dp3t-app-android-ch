package ch.admin.bag.dp3t.checkin.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ch.admin.bag.dp3t.R
import ch.admin.bag.dp3t.checkin.generateqrcode.GenerateQrCodeFragment
import ch.admin.bag.dp3t.checkin.generateqrcode.QRCodeViewModel
import ch.admin.bag.dp3t.checkin.generateqrcode.QrCodeFragment
import ch.admin.bag.dp3t.databinding.FragmentEventsOverviewBinding
import org.crowdnotifier.android.sdk.model.VenueInfo

class EventsOverviewFragment : Fragment() {

	companion object {
		fun newInstance(): EventsOverviewFragment {
			return EventsOverviewFragment()
		}
	}

	private lateinit var binding: FragmentEventsOverviewBinding
	private val qrCodeViewModel: QRCodeViewModel by activityViewModels()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		super.onCreateView(inflater, container, savedInstanceState)
		binding = FragmentEventsOverviewBinding.inflate(layoutInflater)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.eventsToolbar.setNavigationOnClickListener { requireActivity().supportFragmentManager.popBackStack() }

		val adapter = QrCodeAdapter(object : OnClickListener {
			override fun generateQrCode() {
				showGenerateQrCodeScreen()
			}

			override fun onQrCodeClicked(qrCodeItem: VenueInfo) {
				showQrCodeFragment(qrCodeItem)
			}

			override fun onDeleteQrCodeClicked(qrCodeItem: VenueInfo) {
				qrCodeViewModel.deleteQrCode(qrCodeItem)
			}

		})

		binding.qrList.adapter = adapter
		qrCodeViewModel.generatedQrCodesLiveData.observe(viewLifecycleOwner, {
			adapter.setItems(it)
		})
	}

	private fun showQrCodeFragment(venueInfo: VenueInfo) {
		qrCodeViewModel.selectedQrCode = venueInfo
		requireActivity().supportFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.main_fragment_container, QrCodeFragment.newInstance())
			.addToBackStack(QrCodeFragment::class.java.canonicalName)
			.commit()
	}

	private fun showGenerateQrCodeScreen() {
		requireActivity().supportFragmentManager.beginTransaction()
			.setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit, R.anim.slide_pop_enter, R.anim.slide_pop_exit)
			.replace(R.id.main_fragment_container, GenerateQrCodeFragment.newInstance())
			.addToBackStack(GenerateQrCodeFragment::class.java.canonicalName)
			.commit()
	}

}