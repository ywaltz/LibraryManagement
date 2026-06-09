import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LibraryManagerTest1 {
    private LibraryManager manager;
    private LibraryRepository repository;
    private User currentUser;

    @BeforeEach
    void setUp() {
        // 테스트용 레포지토리와 매니저 초기화
        repository = new LibraryRepository();
        manager = new LibraryManager(repository);

        // 매니저 초기화 (파일 로드)
        manager.initialize();

        // 테스트를 위한 초기 데이터 강제 주입 (필요 시)
        // 실제 파일 없이 로직만 테스트하고 싶다면 Mock 객체를 사용하거나
        // 테스트용 도서를 직접 등록합니다.
        manager.getBookMap().clear();
        manager.addBook("테스트 자바", "저자A"); // ID: 1
    }

    @Test
    @DisplayName("로그인 성공 및 실패 테스트")
    void login() {
        // Given: users.csv에 admin/1111 데이터가 있다고 가정
        // When & Then
        assertTrue(manager.login("admin", "1111"), "관리자 로그인이 성공해야 합니다.");
        assertFalse(manager.login("admin", "wrong"), "비밀번호가 틀리면 실패해야 합니다.");
    }

    @Test
    @DisplayName("현재 로그인한 사용자 정보 확인")
    void getCurrentUser() {
        manager.login("admin", "1111");
        User user = manager.getCurrentUser();

        assertNotNull(user);
        assertEquals("admin", user.getUserId());
        assertTrue(user.isAdmin());
    }

    @Test
    @DisplayName("새로운 도서 등록 확인")
    void addBook() {
        int beforeSize = manager.getAllBooks().size();
        manager.addBook("새로운 책", "새로운 저자");

        assertEquals(beforeSize + 1, manager.getAllBooks().size());

        int target_id = manager.getBookCount();
        Book book = manager.getBookMap().get(target_id);
        assertEquals("새로운 책", book.getTitle());
    }

    @Test
    @DisplayName("도서 삭제 확인")
    void deleteBook() {
        // ID 1번 도서 삭제
        int target_id = manager.getBookCount();
        boolean result = manager.deleteBook(target_id);

        assertTrue(result);
        assertNull(manager.getBookMap().get(target_id));
    }

    /**
     * LibraryManager의 보안 취약점을 검증하기 위한 테스트 클래스입니다.
     * <p>주로 인증 로직 및 사용자 권한 제어와 관련된 취약점을 다룹니다.</p>
     * * @author Suman Nam
     * @see LibraryManager#login(String, String)
     *
     * @see <a href="https://github.com/sumannam/Java/issues/44">Issue #44: 보안 취약점 관련 단위 테스트 개발</a>
     */
    @Test
    @DisplayName("도서 대출 로직 확인")
    void borrowBook() {
        manager.login("user", "2222"); // 대출자 로그인

        // 성공 케이스
        int target_id = manager.getBookCount();
        boolean success = manager.borrowBook(target_id);
        assertTrue(success);
        assertFalse(manager.getBookMap().get(target_id).isAvailable());
        assertEquals("user", manager.getBookMap().get(target_id).getBorrowerId());

        // 실패 케이스 (이미 대출 중인 도서)
        boolean fail = manager.borrowBook(1);
        assertFalse(fail);
    }

    @Test
    @DisplayName("도서 반납 로직 확인")
    void returnBook() {
        manager.login("user", "2222");
        manager.borrowBook(1); // 먼저 대출

        // 반납 실행
        int target_id = manager.getBookCount();
        manager.borrowBook(target_id);

        boolean result = manager.returnBook(target_id);
        assertTrue(result);
        assertTrue(manager.getBookMap().get(target_id).isAvailable());
        assertEquals("null", manager.getBookMap().get(target_id).getBorrowerId());
    }

    @Test
    @DisplayName("키워드 기반 도서 검색 확인")
    void searchBook() {
        manager.addBook("파이썬 입문", "저자B");

        List<Book> results = manager.searchBook("자바");
        assertEquals(1, results.size());
        assertEquals("테스트 자바", results.get(0).getTitle());
    }

    @Test
    @DisplayName("전체 도서 목록 반환 확인")
    void getAllBooks() {
        Collection<Book> books = manager.getAllBooks();
        assertNotNull(books);
        assertFalse(books.isEmpty());
    }

    @Test
    @DisplayName("보안 테스트: SQL Injection 공격 문자열 로그인 실패 확인")
    void loginSqlInjectionTest() {
        String attackId = "admin";
        String attackPw = "' OR '1'='1";

        boolean result = manager.login(attackId, attackPw);

        assertFalse(result, "SQL Injection 공격 문자열로 인증이 우회되면 안 됩니다.");
    }

    @Test
    @DisplayName("보안 테스트: OS Command Injection 위험 입력값 차단")
    void osCommandInjectionTest() {
        String attackInput = "127.0.0.1; whoami";

        boolean result = manager.isSafeServerAddress(attackInput);

        assertFalse(result, "OS Command Injection 위험 입력값은 허용되면 안 됩니다.");
    }

    @Test
    @DisplayName("서버 상태 점검 입력값 검증: 정상 IP와 도메인 허용")
    void safeServerAddressShouldBeAllowed() {
        assertTrue(manager.isSafeServerAddress("192.168.100.20"), "정상 IP 주소는 허용되어야 합니다.");
        assertTrue(manager.isSafeServerAddress("google.com"), "정상 도메인 주소는 허용되어야 합니다.");
    }
}