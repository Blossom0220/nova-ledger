package com.nova.ledger.controller;

import com.nova.ledger.dto.ApiResponse;
import com.nova.ledger.entity.Book;
import com.nova.ledger.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ledger/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Book>>> getBooks(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(bookService.getUserBooks(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Book>> getBook(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(bookService.getBook(id, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Book>> createBook(@Valid @RequestBody Book book, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        book.setUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("账本创建成功", bookService.createBook(book)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Book>> updateBook(@PathVariable Long id,
                                                         @Valid @RequestBody Book book,
                                                         Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("账本更新成功", bookService.updateBook(id, userId, book)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        bookService.deleteBook(id, userId);
        return ResponseEntity.ok(ApiResponse.success("账本删除成功", null));
    }
}
