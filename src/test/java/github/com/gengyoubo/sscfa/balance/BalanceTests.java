package github.com.gengyoubo.sscfa.balance;

/** 依次运行 balance 的四组独立测试（都是 main 方法形式，任何一组失败都会抛异常终止）。 */
public final class BalanceTests {
    private BalanceTests() {}

    public static void main(String[] args) throws Exception {
        BalanceFrameworkTest.main(args);
        BalanceReviewTest.main(args);
        BalanceSyncTest.main(args);
        SscBalanceSchemaPinTest.main(args);
        System.out.println("Balance: all four suites passed.");
    }
}
