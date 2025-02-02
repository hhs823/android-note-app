package com.survivalcoding.noteapp.presentation.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.survivalcoding.noteapp.App
import com.survivalcoding.noteapp.R
import com.survivalcoding.noteapp.databinding.FragmentNotesBinding
import com.survivalcoding.noteapp.domain.model.Note
import com.survivalcoding.noteapp.domain.model.SortKey
import com.survivalcoding.noteapp.domain.model.SortMode
import com.survivalcoding.noteapp.presentation.add_edit_note.AddEditNoteFragment
import com.survivalcoding.noteapp.presentation.notes.adapter.NoteListAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<NotesViewModel> {
        NotesViewModelFactory((requireActivity().application as App).noteRepository)
    }
    private val noteListAdapter by lazy {
        NoteListAdapter(
            deleteClickEvent = { note ->
                viewModel.onEvent(NotesEvent.DeleteNote(note))
                Snackbar.make(binding.root, "노트가 삭제되었습니다.", Snackbar.LENGTH_LONG)
                    .setAction("cancel") { viewModel.onEvent(NotesEvent.UndoDelete) }
                    .setAnchorView(binding.fabAddNewNoteButton)
                    .show()
            },
            itemClickEvent = { note ->
                toAddEditNoteFragment(note)
            },
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 리사이클러뷰 연결
        binding.rvNotesRecyclerView.adapter = noteListAdapter

        // title, timestamp, color 기준으로 정렬
        binding.rgSortBase.setOnCheckedChangeListener { _, checkedId ->
            viewModel.onEvent(idToSetKeyEvent(checkedId))
        }

        // 오름차순, 내림차순 정렬
        binding.rgSortMode.setOnCheckedChangeListener { _, checkedId ->
            viewModel.onEvent(idToSetModeEvent(checkedId))
        }

        // 정렬 기능 표시/비표시
        binding.ivDrawerTrigger.setOnClickListener {
            viewModel.onEvent(
                NotesEvent.SetVisibility(
                    when {
                        binding.clSortCondition.isVisible -> View.GONE
                        else -> View.VISIBLE
                    }
                )
            )
        }

        // 노트 추가 버튼 설정
        binding.fabAddNewNoteButton.setOnClickListener {
            toAddEditNoteFragment(Note())
        }

        collect()
    }

    private fun collect() {
        repeatOnStart {
            viewModel.uiState.collectLatest {
                noteListAdapter.submitList(it.notes)
                binding.rgSortBase.check(it.sortKey.toId())
                binding.rgSortMode.check(it.sortMode.toId())
                binding.clSortCondition.visibility = it.visibility
            }
        }
    }

    private fun repeatOnStart(block: suspend CoroutineScope.() -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED, block)
        }
    }

    private fun idToSetKeyEvent(id: Int): NotesEvent.SetSortKey {
        val key = when (id) {
            binding.rbBaseTitle.id -> SortKey.TITLE
            binding.rbBaseDate.id -> SortKey.TIMESTAMP
            else -> SortKey.COLOR
        }
        return NotesEvent.SetSortKey(key)
    }

    private fun idToSetModeEvent(id: Int): NotesEvent.SetSortMode {
        val mode = when (id) {
            binding.rbModeAsc.id -> SortMode.ASCENDING
            else -> SortMode.DESCENDING
        }
        return NotesEvent.SetSortMode(mode)
    }

    private fun toAddEditNoteFragment(note: Note) {
        val bundle = Bundle().apply {
            putParcelable(NOTE_KEY, note)
        }
        parentFragmentManager.commit {
            replace(R.id.fragmentContainerView, AddEditNoteFragment().apply { arguments = bundle })
            setReorderingAllowed(true)
            addToBackStack(null)
        }
    }

    companion object {
        const val NOTE_KEY = "note_key"
    }
}
