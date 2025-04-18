package community.flock.wirespec.example.maven.custom.app.user;

import java.util.List;

public interface UserContext extends UserAdapter.Module {
    class Service {
        private Service() {
        }

        public static List<User> getAllUsers(final UserContext ctx, final String name) {
            return ctx.userAdapter().getAllUsers(name);
        }

        public static User getUserByName(final UserContext ctx, final String name) {
            return ctx.userAdapter().getUserByName(name);
        }

        public static User saveUser(final UserContext ctx, final User user) {
            return ctx.userAdapter().saveUser(user);
        }

        public static User deleteUserByName(final UserContext ctx, final String name) {
            return ctx.userAdapter().deleteUserByName(name);
        }

        public static void uploadImageByName(final UserContext ctx, final String name, final byte[] bytes) {
            ctx.userAdapter().uploadImage(name, bytes);

        }
    }
}
