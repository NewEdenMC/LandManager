package co.neweden.LandManager;

import java.util.UUID;

/*
 This class is used in place of the Protection class when no Protection exists for a Block/Entity when calling
 Protections.getACL()
 */
public class FallbackACL extends ACL {

    private ACL parent;

    private FallbackACL() { }

    protected FallbackACL(ACL parent) {
        this.parent = parent;
    }

    @Override
    public ACLSet getACL() {
        ACLSet acl = new ACLSet();
        if (getParentACL() != null) {
            getParentACL().getACL().forEach(e -> {
                if (e.level.equals(Level.ENTER)) return;
                acl.add(new ACLEntry(e.uuid, e.level, true));
            });
        }
        acl.add(new ACLEntry(null, getEveryoneAccessLevel(), false));
        return acl;
    }

    public ACL getParentACL() { return parent; }

    public UUID getOwner() {
        throw new UnsupportedOperationException("Method getOwner() is not supported for a ContextualACL, when calling getOwner() you should ensure ACL is not an instance of ContextualACL");
    }

    public Level getEveryoneAccessLevel() {
        if (getParentACL() == null)
            return Level.FULL_ACCESS;
        return (getParentACL().getEveryoneAccessLevel().equals(Level.ENTER)) ? Level.NO_ACCESS : getParentACL().getEveryoneAccessLevel();
    }

    public boolean setEveryoneAccessLevel(Level level) {
        throw new UnsupportedOperationException("Method setEveryoneAccessLevel() is not supported for a ContextualACL, when calling setEveryoneAccessLevel() you should ensure ACL is not an instance of ContextualACL");
    }

    public boolean setAccess(UUID uuid, Level level) {
        throw new UnsupportedOperationException("Method setAccess() is not supported for a ContextualACL, when calling setAccess() you should ensure ACL is not an instance of ContextualACL");
    }

}
