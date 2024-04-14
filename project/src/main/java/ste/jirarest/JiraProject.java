package ste.jirarest;

import ste.jirarest.util.Http;
import ste.jirarest.util.Parsing;

import org.json.JSONObject;

public final class JiraProject {
    public final class AvatarUrls {
        private final String k48x48;
        private final String k24x24;
        private final String k16x16;
        private final String k32x32;

        private AvatarUrls(JSONObject o) {
            k16x16 = o.getString("16x16");
            k24x24 = o.getString("24x24");
            k32x32 = o.getString("32x32");
            k48x48 = o.getString("48x48");
        }

        public String get48x48() {
            return k48x48;
        }

        public String get32x32() {
            return k32x32;
        }

        public String get24x24() {
            return k24x24;
        }
 
        public String get16x16() {
            return k16x16;
        }
    }

    public final class Lead {
        private final String self;
        private final String key;
        private final String name;
        private final AvatarUrls avatarUrls;
        private final String displayName;
        private final boolean active;

        private Lead(JSONObject o) {
            self = o.getString("self");
            key = o.getString("key");
            name = o.getString("name");
            avatarUrls = new AvatarUrls(o.getJSONObject("avatarUrls"));
            displayName = o.getString("displayName");
            active = o.getBoolean("active");
        }

        public String getSelf() {
            return self;    
        }

        public String getKey() {
            return key;    
        }
        
        public String getName() {
            return name;    
        }
        
        public AvatarUrls getAvatarUrls() {
            return avatarUrls;    
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isActive() {
            return active;    
        }
    }

    public final class Roles {
        private final String developers;
        private final String contributors;
        private final String pmc;
        private final String committers;
        private final String administrators;
        private final String asfMembers;
        private final String users;
        private final String contributors1;
    
        private Roles(JSONObject o) {
            developers = o.getString("Developers");
            contributors = o.getString("Contributors");
            pmc = o.getString("PMC");
            committers = o.getString("Commiters");
            administrators = o.getString("Administrators");
            asfMembers = o.getString("ASF Members");
            users = o.getString("Users");
            contributors1 = o.getString("Contributors 1");
        }
        
        public String getAdministrators() {
            return administrators;
        }

        public String getAsfMembers() {
            return asfMembers;
        }

        public String getCommitters() {
            return committers;
        }

        public String getContributors() {
            return contributors;
        }

        public String getContributors1() {
            return contributors1;
        }

        public String getDevelopers() {
            return developers;
        }

        public String getPmc() {
            return pmc;
        }

        public String getUsers() {
            return users;
        }
    }

    public final class ProjectCategory {
        private final String self;
        private final String id;
        private final String name;
        private final String description;

        private ProjectCategory(JSONObject o) {
            self = o.getString("self");
            id = o.getString("id");
            name = o.getString("name");
            description = o.getString("description");
        }
        
        public String getSelf() {
            return self;
        }

        public String getDescription() {
            return description;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public final class Component {
        private final String self;
        private final String id;
        private final String name;
        private final String description;
        private final boolean assigneeTypeValid;

        private Component(JSONObject o) {
            self = o.getString("self");
            id = o.getString("id");
            name = o.getString("name");
            description = o.getString("description");
            assigneeTypeValid = o.getBoolean("isAssigneeTypeValid");
        }

        public String getDescription() {
            return description;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
        
        public String getSelf() {
            return self;
        }

        public boolean isAssigneeTypeValid() {
            return assigneeTypeValid;
        }
    }

    public final class IssueType {
        private final String self;
        private final String id;
        private final String description;
        private final String iconUrl;
        private final String name;
        private final boolean subtask;
        private final int avatarId;

        private IssueType(JSONObject o) {
            self = o.getString("self");
            id = o.getString("id");
            description = o.getString("description");
            iconUrl = o.getString("iconUrl");
            name = o.getString("name");
            subtask = o.getBoolean("subtask");
            avatarId = o.getInt("avatarId");
        }

        public int getAvatarId() {
            return avatarId;
        }

        public String getDescription() {
            return description;
        }

        public String getIconUrl() {
            return iconUrl;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSelf() {
            return self;
        }

        public boolean isSubtask() {
            return subtask;
        }
    }

    public final class Version {
        private final String self;
        private final String id;
        private final String name;
        private final boolean archived;
        private final boolean released;
        private final String startDate;
        private final String releaseDate;
        private final boolean overdue;
        private final String userStartDate;
        private final String userReleaseDate;
        private final int projectId;

        private Version(JSONObject o) {
            self = o.getString("self");
            id = o.getString("id");
            name = o.getString("name");
            archived = o.getBoolean("archived");
            released = o.getBoolean("released");
            releaseDate = o.getString("releaseDate");
            userReleaseDate = o.getString("userReleaseDate");
            projectId = o.getInt("projectId");
            startDate = (String) Parsing.maybeGet(o, "startDate", Parsing.M_GET_STRING);
            overdue = (boolean) Parsing.maybeGet(o, "overdue", Parsing.M_GET_BOOLEAN);
            userStartDate = (String) Parsing.maybeGet(o, "userStartDate", Parsing.M_GET_STRING);
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getProjectId() {
            return projectId;
        }

        public String getReleaseDate() {
            return releaseDate;
        }

        public String getSelf() {
            return self;
        }

        public String getUserReleaseDate() {
            return userReleaseDate;
        }

        public boolean isArchived() {
            return archived;
        }

        public boolean isReleased() {
            return released;
        }

        public String getStartDate() {
            return startDate;
        }

        public String getUserStartDate() {
            return userStartDate;
        }

        public boolean isOverdue() {
            return overdue;
        }
    }

    private final String expand;
    private final String self;
    private final String id;
    private final String key;
    private final String description;
    private final Lead lead;
    private final Component[] components;
    private final IssueType[] issueTypes;
    private final String url;
    private final String assigneeType;
    private final Version[] versions;
    private final String name;
    private final Roles roles;
    private final AvatarUrls avatarUrls;
    private final ProjectCategory projectCategory;
    private final String projectTypeKey;
    private final boolean archived;

    private JiraProject(JSONObject o) {
        expand = o.getString("expand");
        self = o.getString("self");
        id = o.getString("id");
        key = o.getString("key");
        description = o.getString("description");
        lead = new Lead(o.getJSONObject("lead"));
        url = o.getString("url");
        assigneeType = o.getString("assigneeType");
        name = o.getString("name");
        roles = new Roles(o.getJSONObject("roles"));
        avatarUrls = new AvatarUrls(o.getJSONObject("avatarUrls"));
        projectCategory = new ProjectCategory(o.getJSONObject("projectCategory"));
        projectTypeKey = o.getString("projectTypeKey");
        archived = o.getBoolean("archived");
        components = (Component[]) Parsing.getArray(o.getJSONArray("components"), Component.class);
        issueTypes = (IssueType[]) Parsing.getArray(o.getJSONArray("issueTypes"), IssueType.class);
        versions = (Version[]) Parsing.getArray(o.getJSONArray("versions"), Version.class);
    }
    
    public String getAssigneeType() {
        return assigneeType;
    }

    public AvatarUrls getAvatarUrls() {
        return avatarUrls;
    }

    public Component[] getComponents() {
        return components;
    }

    public String getDescription() {
        return description;
    }
    
    public String getExpand() {
        return expand;
    }

    public String getId() {
        return id;
    }

    public IssueType[] getIssueTypes() {
        return issueTypes;
    }

    public String getKey() {
        return key;
    }

    public Lead getLead() {
        return lead;
    }

    public String getName() {
        return name;
    }

    public ProjectCategory getProjectCategory() {
        return projectCategory;
    }

    public String getProjectTypeKey() {
        return projectTypeKey;
    }

    public Roles getRoles() {
        return roles;
    }

    public String getSelf() {
        return self;
    }

    public String getUrl() {
        return url;
    }

    public Version[] getVersions() {
        return versions;
    }

    public boolean isArchived() {
        return archived;
    }

    public static JiraProject getProjectByName(String name) throws Http.RequestException {
        String ucName = name.toUpperCase();
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder
            .append("https://issues.apache.org/jira/rest/api/2/project/")
            .append(ucName);

        String jsonData = Http.get(urlBuilder.toString());
        return new JiraProject(new JSONObject(jsonData));
    }
}