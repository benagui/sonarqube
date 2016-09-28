/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualityprofile;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.component.Component;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.user.UserSession;

@ServerSide
@ComputeEngineSide
public class QProfileProjectLookup {

  private final DbClient db;
  private final UserSession userSession;

  public QProfileProjectLookup(DbClient db, UserSession userSession) {
    this.db = db;
    this.userSession = userSession;
  }

  public List<Component> projects(int profileId) {
    DbSession dbSession = db.openSession(false);
    try {
      QualityProfileDto qualityProfile = db.qualityProfileDao().selectById(dbSession, profileId);
      QProfileValidations.checkProfileIsNotNull(qualityProfile);
      Map<String, Component> componentsByKeys = Maps.newHashMap();
      for (Component component : db.qualityProfileDao().selectProjects(qualityProfile.getName(), qualityProfile.getLanguage(), dbSession)) {
        componentsByKeys.put(component.key(), component);
      }

      List<Component> result = Lists.newArrayList();
      Collection<String> authorizedProjectKeys = db.permissionDao().selectAuthorizedRootProjectsKeys(dbSession, userSession.getUserId(), UserRole.USER);
      for (Map.Entry<String, Component> entry : componentsByKeys.entrySet()) {
        if (authorizedProjectKeys.contains(entry.getKey())) {
          result.add(entry.getValue());
        }
      }

      return result;
    } finally {
      db.closeSession(dbSession);
    }
  }

  public int countProjects(QProfile profile) {
    return db.qualityProfileDao().countProjects(profile.name(), profile.language());
  }

  @CheckForNull
  public QProfile findProfileByProjectAndLanguage(long projectId, String language) {
    QualityProfileDto dto = db.qualityProfileDao().selectByProjectAndLanguage(projectId, language);
    if (dto != null) {
      return QProfile.from(dto);
    }
    return null;
  }

}
