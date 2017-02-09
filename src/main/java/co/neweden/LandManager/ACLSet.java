package co.neweden.LandManager;

import java.util.*;
import java.util.stream.Collectors;

/*
 An implementation of HashSet specifically designed for making handling
 Access Control Lists easier.

 Adding/removing ACL entries here does not modify the underlying Access
 Control List stored in the database, it only effects the current instance
 of this Java Set you are working with.  To modify the actual underlying
 Access Control List use the ACL.setAccess method.

 By rule Java Sets can't contain duplicate keys, which is what we want
 but we want to consider ACL.Entry.uuid as the key because that makes
 the most sense for what we want to achieve.
 */

public class ACLSet extends TreeSet<ACLEntry> {

    @Override
    public boolean add(ACLEntry entry) {
        removeAll(stream().filter(e -> e.equalsUUID(entry.uuid)).collect(Collectors.toSet()));
        return super.add(entry);
    }

    @Override
    public boolean addAll(Collection<? extends ACLEntry> entries) {
        for (ACLEntry entry : entries) {
            if (!add(entry)) return false;
        }
        return true;
    }

    public boolean remove(UUID uuid) {
        return super.removeAll(stream().filter(e -> e.equalsUUID(uuid)).collect(Collectors.toSet()));
    }

    @Override
    public boolean removeAll(Collection<?> col) {
        boolean rres = true;
        for (Object obj : col) {
            boolean res;
            if (obj instanceof UUID)
                res = remove((UUID) obj);
            else
                res = remove(obj);
            if (!res) rres = false;
        }
        return rres;
    }

    public boolean contains(UUID uuid) { return (get(uuid) != null); }

    public ACLEntry get(UUID uuid) {
        Optional<ACLEntry> opt = stream().filter(e -> e.equalsUUID(uuid)).findFirst();
        return (opt.isPresent()) ? opt.get() : null;
    }

    @Override
    public String toString() {
        String list = "";
        for (ACLEntry e : this) {
            list += "   " + e + "\n";
        }
        return "[\n" + list + "]";
    }

}
