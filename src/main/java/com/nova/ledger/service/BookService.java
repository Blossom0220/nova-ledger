package com.nova.ledger.service;

import com.nova.ledger.entity.Book;
import com.nova.ledger.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    public List<Book> getUserBooks(Long userId) {
        return bookRepository.findByUserIdAndDeletedFalseOrderBySortOrderAsc(userId);
    }

    public Book getBook(Long id, Long userId) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("账本不存在"));
        if (!book.getUserId().equals(userId) || book.getDeleted()) {
            throw new RuntimeException("无权访问该账本");
        }
        return book;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "categoryTree", key = "#book.userId + ':' + #book.id"),
            @CacheEvict(value = "stats", key = "#book.userId + ':' + #book.id")
    })
    public Book createBook(Book book) {
        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBook(Long id, Long userId, Book update) {
        Book book = getBook(id, userId);
        book.setName(update.getName());
        book.setDescription(update.getDescription());
        book.setCurrency(update.getCurrency());
        book.setCoverColor(update.getCoverColor());
        book.setSortOrder(update.getSortOrder());
        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(Long id, Long userId) {
        Book book = getBook(id, userId);
        book.setDeleted(true);
        bookRepository.save(book);
    }
}
