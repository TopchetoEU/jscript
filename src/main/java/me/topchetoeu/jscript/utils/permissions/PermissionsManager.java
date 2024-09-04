package me.topchetoeu.jscript.utils.permissions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class PermissionsManager implements PermissionsProvider {
    public final ArrayList<PermissionPredicate> predicates = new ArrayList<>();

    public PermissionsProvider add(PermissionPredicate perm) {
        predicates.add(perm);
        return this;
    }
    public PermissionsProvider add(String perm) {
        predicates.add(new PermissionPredicate(perm));
        return this;
    }

    @Override public boolean hasPermission(Permission perm, String value) {
        for (var el : predicates) {
            if (el.match(perm, value)) {
                if (el.denies) return false;
                else return true;
            }
        }

        return false;
    }
    @Override public boolean hasPermission(Permission perm) {
        for (var el : predicates) {
            if (el.match(perm)) {
                if (el.denies) return false;
                else return true;
            }
        }

        return false;
    }

    public PermissionsProvider addFromStream(InputStream stream) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(stream));
        String line;

        while ((line = reader.readLine()) != null) {
            var i = line.indexOf('#');
            if (i >= 0) line = line.substring(0, i);

            line = line.trim();

            if (line.isEmpty()) continue;

            add(line);
        }

        return this;
    }
}
