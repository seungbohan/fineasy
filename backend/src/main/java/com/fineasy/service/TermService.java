package com.fineasy.service;

import com.fineasy.dto.response.FinancialTermResponse;
import com.fineasy.dto.response.TermCategoryResponse;
import com.fineasy.entity.FinancialTermEntity;
import com.fineasy.entity.TermCategoryEntity;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.repository.FinancialTermRepository;
import com.fineasy.repository.TermCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TermService {

    private final FinancialTermRepository termRepository;

    private final TermCategoryRepository categoryRepository;

    public TermService(FinancialTermRepository termRepository,
                       TermCategoryRepository categoryRepository) {
        this.termRepository = termRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<FinancialTermResponse> getAllTerms() {
        return termRepository.findAllOrderByName().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<FinancialTermResponse> searchTerms(String query) {
        return termRepository.searchByName(query).stream()
                .map(this::toResponse)
                .toList();
    }

    public FinancialTermResponse getTermById(long termId) {
        FinancialTermEntity term = termRepository.findById(termId)
                .orElseThrow(() -> new EntityNotFoundException("FinancialTerm", termId));
        return toDetailedResponse(term);
    }

    public List<TermCategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(c -> new TermCategoryResponse(
                        c.getId(),
                        c.getName(),
                        c.getDisplayOrder() != null ? c.getDisplayOrder() : 0))
                .toList();
    }

    public List<FinancialTermResponse> getTermsByCategory(long categoryId) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("TermCategory", categoryId));

        return termRepository.findByCategoryId(categoryId).stream()
                .map(this::toResponse)
                .toList();
    }

    private FinancialTermResponse toResponse(FinancialTermEntity term) {
        String categoryName = term.getCategory() != null
                ? term.getCategory().getName()
                : "Unknown";

        return new FinancialTermResponse(
                term.getId(),
                term.getName(),
                term.getNameEn(),
                categoryName,
                term.getDifficulty(),
                term.getSimpleDescription(),
                term.getDetailedDescription(),
                term.getExampleSentence(),
                List.of()
        );
    }

    private FinancialTermResponse toDetailedResponse(FinancialTermEntity term) {
        String categoryName = term.getCategory() != null
                ? term.getCategory().getName()
                : "Unknown";

        List<FinancialTermResponse.RelatedTerm> relatedTerms = term.getRelatedTerms().stream()
                .map(rt -> new FinancialTermResponse.RelatedTerm(rt.getId(), rt.getName()))
                .toList();

        return new FinancialTermResponse(
                term.getId(),
                term.getName(),
                term.getNameEn(),
                categoryName,
                term.getDifficulty(),
                term.getSimpleDescription(),
                term.getDetailedDescription(),
                term.getExampleSentence(),
                relatedTerms
        );
    }
}
