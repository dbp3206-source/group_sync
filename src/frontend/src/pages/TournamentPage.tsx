import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  getGroups,
  getGroup,
  type GroupDetail,
  type GroupSummary,
} from "../api/groups";
import {
  getBadmintonSeasons,
  getBadmintonSessions,
  type BadmintonSeason,
  type BadmintonSession,
} from "../api/badminton";
import {
  addTournamentEntry,
  createTournament,
  getTournamentBracket,
  getTournamentEntries,
  getTournaments,
  openTournament,
  recordTournamentWinner,
  startTournament,
  type Bracket,
  type Tournament,
  type TournamentEntry,
} from "../api/tournaments";
import { getApiErrorMessage } from "../api/errors";
import { useAuth } from "../auth/AuthContext";

function TournamentPage() {
  const { user } = useAuth();
  const [params] = useSearchParams();
  const [groups, setGroups] = useState<GroupSummary[]>([]);
  const [groupId, setGroupId] = useState(Number(params.get("groupId") ?? 0));
  const [group, setGroup] = useState<GroupDetail | null>(null);
  const [seasons, setSeasons] = useState<BadmintonSeason[]>([]);
  const [sessions, setSessions] = useState<BadmintonSession[]>([]);
  const [tournaments, setTournaments] = useState<Tournament[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [entries, setEntries] = useState<TournamentEntry[]>([]);
  const [bracket, setBracket] = useState<Bracket[]>([]);
  const [createOpen, setCreateOpen] = useState(false);
  const [entryOpen, setEntryOpen] = useState(false);
  const [name, setName] = useState("");
  const [seasonId, setSeasonId] = useState(0);
  const [sessionId, setSessionId] = useState(0);
  const [competitionMode, setCompetitionMode] = useState<"SINGLES" | "DOUBLES">(
    "SINGLES",
  );
  const [maxEntries, setMaxEntries] = useState(8);
  const [selectedMembers, setSelectedMembers] = useState<number[]>([]);
  const [entryName, setEntryName] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const selectedTournament =
    tournaments.find((item) => item.id === selectedId) ?? null;
  const expectedPlayers =
    selectedTournament?.competitionMode === "DOUBLES" ? 2 : 1;
  const canManage =
    group?.members.some(
      (member) =>
        member.userId === user?.id &&
        (member.role === "OWNER" || member.role === "ORGANIZER"),
    ) ?? false;
  async function loadGroup(groupIdToLoad: number) {
    const [nextGroup, nextSeasons, nextSessions, nextTournaments] =
      await Promise.all([
        getGroup(groupIdToLoad),
        getBadmintonSeasons(groupIdToLoad),
        getBadmintonSessions(groupIdToLoad),
        getTournaments(groupIdToLoad),
      ]);
    setGroup(nextGroup);
    setSeasons(nextSeasons);
    setSessions(nextSessions);
    setTournaments(nextTournaments);
    setSeasonId(nextSeasons[0]?.id ?? 0);
    setSessionId(
      nextSessions.find(
        (session) =>
          session.status === "CONFIRMED" || session.status === "PLAYING",
      )?.id ??
        nextSessions[0]?.id ??
        0,
    );
    setSelectedId(nextTournaments[0]?.id ?? null);
  }
  useEffect(() => {
    getGroups()
      .then((items) => {
        const badminton = items.filter((group) => group.type === "BADMINTON");
        setGroups(badminton);
        if (!groupId) setGroupId(badminton[0]?.id ?? 0);
      })
      .catch((requestError) =>
        setError(
          getApiErrorMessage(requestError, "Không thể tải nhóm cầu lông."),
        ),
      );
  }, []);
  useEffect(() => {
    if (groupId)
      loadGroup(groupId).catch((requestError) =>
        setError(getApiErrorMessage(requestError, "Không thể tải tournament.")),
      );
  }, [groupId]);
  useEffect(() => {
    if (!selectedId) {
      setEntries([]);
      setBracket([]);
      return;
    }
    Promise.all([
      getTournamentEntries(selectedId),
      getTournamentBracket(selectedId),
    ])
      .then(([nextEntries, nextBracket]) => {
        setEntries(nextEntries);
        setBracket(nextBracket);
      })
      .catch((requestError) =>
        setError(getApiErrorMessage(requestError, "Không thể tải nhánh đấu.")),
      );
  }, [selectedId]);
  const occupiedUserIds = useMemo(
    () =>
      new Set(
        entries.flatMap((entry) =>
          entry.members.map((member) => member.userId),
        ),
      ),
    [entries],
  );

  async function create(event: React.FormEvent) {
    event.preventDefault();
    setError("");
    setMessage("");
    try {
      const tournament = await createTournament(groupId, {
        name,
        seasonId,
        sessionId,
        competitionMode,
        maxEntries,
      });
      setName("");
      setCreateOpen(false);
      setMessage("Đã tạo tournament ở trạng thái nháp.");
      await loadGroup(groupId);
      setSelectedId(tournament.id);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Không thể tạo tournament."));
    }
  }
  async function addEntry(event: React.FormEvent) {
    event.preventDefault();
    if (!selectedTournament) return;
    setError("");
    setMessage("");
    if (selectedMembers.length !== expectedPlayers) {
      setError(`Hãy chọn đúng ${expectedPlayers} người chơi.`);
      return;
    }
    try {
      await addTournamentEntry(selectedTournament.id, {
        displayName: entryName || undefined,
        memberIds: selectedMembers,
        seedNumber: entries.length + 1,
      });
      setSelectedMembers([]);
      setEntryName("");
      setEntryOpen(false);
      setMessage("Đã thêm entry vào tournament.");
      setEntries(await getTournamentEntries(selectedTournament.id));
      await loadGroup(groupId);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Không thể thêm entry."));
    }
  }
  async function action(run: () => Promise<Tournament>, success: string) {
    setError("");
    setMessage("");
    if (!selectedTournament) return;
    try {
      await run();
      setMessage(success);
      await loadGroup(groupId);
      setEntries(await getTournamentEntries(selectedTournament.id));
      setBracket(await getTournamentBracket(selectedTournament.id));
    } catch (requestError) {
      setError(
        getApiErrorMessage(requestError, "Không thể cập nhật tournament."),
      );
    }
  }
  async function recordWinner(match: Bracket, winnerEntryId: number) {
    if (!selectedTournament) return;
    setError("");
    setMessage("");
    try {
      await recordTournamentWinner(
        selectedTournament.id,
        match.id,
        winnerEntryId,
      );
      setMessage("Đã ghi nhận người thắng và cập nhật nhánh kế tiếp.");
      await loadGroup(groupId);
      setBracket(await getTournamentBracket(selectedTournament.id));
    } catch (requestError) {
      setError(
        getApiErrorMessage(
          requestError,
          "Không thể ghi nhận kết quả trận đấu.",
        ),
      );
    }
  }
  function toggleMember(userId: number) {
    setSelectedMembers((current) =>
      current.includes(userId)
        ? current.filter((id) => id !== userId)
        : current.length >= expectedPlayers
          ? [...current.slice(1), userId]
          : [...current, userId],
    );
  }

  return (
    <section className="tournament-page">
      <header className="tournament-hero">
        <div>
          <p className="eyebrow">BADMINTON TOURNAMENT</p>
          <h1>Nhánh đấu của nhóm.</h1>
          <p>
            Tạo giải knockout cho Singles hoặc Doubles. Organizer chốt đội hình,
            seed và khởi tạo bracket một lần.
          </p>
        </div>
        <div className="schedule-actions">
          <select
            className="tournament-group-picker"
            value={groupId}
            onChange={(event) => setGroupId(Number(event.target.value))}
          >
            {groups.map((group) => (
              <option key={group.id} value={group.id}>
                {group.name}
              </option>
            ))}
          </select>
          <button
            className="button button--primary"
            onClick={() => setCreateOpen(true)}
            disabled={!groupId || !canManage}
          >
            Tạo tournament
          </button>
        </div>
      </header>
      {(error || message) && (
        <div
          className={`status-card ${error ? "status-card--error" : "status-card--success"}`}
          role={error ? "alert" : "status"}
        >
          {error || message}
        </div>
      )}
      {tournaments.length === 0 ? (
        <div className="tournament-empty">
          <h2>Chưa có tournament nào.</h2>
          <p>
            Chọn một session cầu lông và tạo giải knockout đầu tiên cho nhóm.
          </p>
          <button
            className="button button--primary"
            onClick={() => setCreateOpen(true)}
            disabled={!groupId || !canManage}
          >
            Tạo tournament
          </button>
        </div>
      ) : (
        <>
          <div
            className="tournament-list"
            role="tablist"
            aria-label="Tournament trong nhóm"
          >
            {tournaments.map((tournament) => (
              <button
                key={tournament.id}
                className={selectedId === tournament.id ? "is-selected" : ""}
                onClick={() => setSelectedId(tournament.id)}
              >
                <span>
                  {tournament.competitionMode === "DOUBLES" ? "D" : "S"}
                </span>
                <div>
                  <strong>{tournament.name}</strong>
                  <small>
                    {tournament.competitionMode === "DOUBLES"
                      ? "Doubles"
                      : "Singles"}{" "}
                    · {tournament.entries}/{tournament.maxEntries} entries
                  </small>
                </div>
                <b>{tournament.status.replaceAll("_", " ")}</b>
              </button>
            ))}
          </div>
          {selectedTournament && (
            <div className="tournament-detail">
              <section className="tournament-summary">
                <div>
                  <p className="eyebrow">
                    {selectedTournament.competitionMode === "DOUBLES"
                      ? "DOUBLES"
                      : "SINGLES"}{" "}
                    · KNOCKOUT
                  </p>
                  <h2>{selectedTournament.name}</h2>
                  <p>
                    {selectedTournament.entries}/{selectedTournament.maxEntries}{" "}
                    entries đã sẵn sàng.
                  </p>
                </div>
                <div className="tournament-actions">
                  {selectedTournament.status === "DRAFT" && canManage && (
                    <button
                      className="button button--secondary"
                      onClick={() =>
                        action(
                          () => openTournament(selectedTournament.id),
                          "Đã mở danh sách entry cho organizer.",
                        )
                      }
                    >
                      Mở roster
                    </button>
                  )}
                  {selectedTournament.status === "REGISTRATION_OPEN" &&
                    canManage && (
                      <>
                        <button
                          className="button button--secondary"
                          onClick={() => setEntryOpen(true)}
                          disabled={
                            entries.length >= selectedTournament.maxEntries
                          }
                        >
                          Thêm entry
                        </button>
                        <button
                          className="button button--primary"
                          onClick={() =>
                            action(
                              () => startTournament(selectedTournament.id),
                              "Đã sinh nhánh knockout tự động.",
                            )
                          }
                        >
                          Khóa roster & tạo bracket
                        </button>
                      </>
                    )}
                  {selectedTournament.status === "IN_PROGRESS" && (
                    <span className="tournament-live">
                      Bracket đang diễn ra
                    </span>
                  )}
                </div>
              </section>
              <div className="tournament-layout">
                <section className="tournament-roster">
                  <div className="panel-heading">
                    <div>
                      <p className="eyebrow">ROSTER</p>
                      <h2>Entry đã chốt</h2>
                    </div>
                    <span>{entries.length}</span>
                  </div>
                  {entries.length ? (
                    <ol>
                      {entries.map((entry) => (
                        <li key={entry.id}>
                          <b>{entry.seedNumber ?? "—"}</b>
                          <div>
                            <strong>{entry.displayName}</strong>
                            <small>
                              {entry.members
                                .map((member) => member.displayName)
                                .join(" · ")}
                            </small>
                          </div>
                        </li>
                      ))}
                    </ol>
                  ) : (
                    <p className="panel-empty">
                      Mở roster để thêm người chơi hoặc đội đôi.
                    </p>
                  )}
                </section>
                <section className="bracket-panel">
                  <div className="panel-heading">
                    <div>
                      <p className="eyebrow">BRACKET</p>
                      <h2>Knockout</h2>
                    </div>
                    <span>
                      {bracket.length ? `${bracket.length} trận` : "Chưa sinh"}
                    </span>
                  </div>
                  {bracket.length ? (
                    <div className="bracket-scroll">
                      <div className="bracket-grid">
                        {bracket.map((match) => (
                          <div
                            className={`bracket-match bracket-match--${match.stage.toLowerCase()}`}
                            key={match.id}
                          >
                            <small>
                              {match.stage === "FINAL"
                                ? "Chung kết"
                                : `Trận ${match.matchNumber}`}{" "}
                              ·{" "}
                              {match.status === "READY"
                                ? "Sẵn sàng"
                                : match.status === "COMPLETED"
                                  ? "Đã xong"
                                  : "Chờ kết quả"}
                            </small>
                            <div>
                              {match.entryA?.displayName ?? "Chờ kết quả"}
                              <b>
                                {match.winnerEntry?.id === match.entryA?.id
                                  ? "✓"
                                  : ""}
                              </b>
                              {canManage && match.status === "READY" && (
                                <button
                                  className="mini-button"
                                  onClick={() =>
                                    recordWinner(match, match.entryA!.id)
                                  }
                                >
                                  Thắng
                                </button>
                              )}
                            </div>
                            <div>
                              {match.entryB?.displayName ?? "Chờ kết quả"}
                              <b>
                                {match.winnerEntry?.id === match.entryB?.id
                                  ? "✓"
                                  : ""}
                              </b>
                              {canManage && match.status === "READY" && (
                                <button
                                  className="mini-button"
                                  onClick={() =>
                                    recordWinner(match, match.entryB!.id)
                                  }
                                >
                                  Thắng
                                </button>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <p className="panel-empty">
                      Sau khi roster được khóa, GroupSync xếp seed và sinh
                      bracket tự động.
                    </p>
                  )}
                </section>
              </div>
            </div>
          )}
        </>
      )}
      {createOpen && (
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={() => setCreateOpen(false)}
        >
          <form
            className="schedule-modal form-stack"
            onSubmit={create}
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="modal-heading">
              <div>
                <p className="eyebrow">TOURNAMENT MỚI</p>
                <h2>Tạo knockout</h2>
              </div>
              <button
                type="button"
                className="modal-close"
                onClick={() => setCreateOpen(false)}
                aria-label="Đóng"
              >
                ×
              </button>
            </div>
            <label htmlFor="tournament-name">
              Tên giải
              <input
                id="tournament-name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                required
                autoFocus
              />
            </label>
            <div className="two-fields">
              <label htmlFor="tournament-season">
                Mùa giải
                <select
                  id="tournament-season"
                  value={seasonId}
                  onChange={(event) => setSeasonId(Number(event.target.value))}
                >
                  {seasons.map((season) => (
                    <option key={season.id} value={season.id}>
                      {season.name}
                    </option>
                  ))}
                </select>
              </label>
              <label htmlFor="tournament-session">
                Session
                <select
                  id="tournament-session"
                  value={sessionId}
                  onChange={(event) => setSessionId(Number(event.target.value))}
                >
                  {sessions.map((session) => (
                    <option key={session.id} value={session.id}>
                      {session.title}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <fieldset className="group-type-choice">
              <legend>Nội dung thi đấu</legend>
              <label
                className={competitionMode === "SINGLES" ? "is-selected" : ""}
              >
                <input
                  type="radio"
                  checked={competitionMode === "SINGLES"}
                  onChange={() => setCompetitionMode("SINGLES")}
                />
                <b>Singles</b>
                <span>Một người cho mỗi entry.</span>
              </label>
              <label
                className={competitionMode === "DOUBLES" ? "is-selected" : ""}
              >
                <input
                  type="radio"
                  checked={competitionMode === "DOUBLES"}
                  onChange={() => setCompetitionMode("DOUBLES")}
                />
                <b>Doubles</b>
                <span>Hai người cho mỗi đội.</span>
              </label>
            </fieldset>
            <label htmlFor="max-entries">
              Số entry tối đa
              <input
                id="max-entries"
                type="number"
                min="2"
                max="64"
                value={maxEntries}
                onChange={(event) => setMaxEntries(Number(event.target.value))}
                required
              />
            </label>
            <div className="modal-actions">
              <button
                className="button button--primary"
                disabled={!seasonId || !sessionId}
              >
                Tạo ở trạng thái nháp
              </button>
            </div>
          </form>
        </div>
      )}
      {entryOpen && selectedTournament && (
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={() => setEntryOpen(false)}
        >
          <form
            className="schedule-modal form-stack"
            onSubmit={addEntry}
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="modal-heading">
              <div>
                <p className="eyebrow">THÊM ENTRY</p>
                <h2>
                  {selectedTournament.competitionMode === "DOUBLES"
                    ? "Ghép đội đôi"
                    : "Chọn người chơi"}
                </h2>
              </div>
              <button
                type="button"
                className="modal-close"
                onClick={() => setEntryOpen(false)}
                aria-label="Đóng"
              >
                ×
              </button>
            </div>
            {selectedTournament.competitionMode === "DOUBLES" && (
              <label htmlFor="entry-name">
                Tên đội <small>(không bắt buộc)</small>
                <input
                  id="entry-name"
                  value={entryName}
                  onChange={(event) => setEntryName(event.target.value)}
                  placeholder="Tự ghép từ tên thành viên"
                />
              </label>
            )}
            <fieldset className="member-picker">
              <legend>
                Chọn {expectedPlayers} người ({selectedMembers.length}/
                {expectedPlayers})
              </legend>
              {group?.members
                .filter((member) => !occupiedUserIds.has(member.userId))
                .map((member) => (
                  <label key={member.userId}>
                    <input
                      type="checkbox"
                      checked={selectedMembers.includes(member.userId)}
                      onChange={() => toggleMember(member.userId)}
                    />
                    <span className="avatar-fallback">
                      {member.displayName.slice(0, 1).toUpperCase()}
                    </span>
                    {member.displayName}
                  </label>
                ))}
            </fieldset>
            <div className="modal-actions">
              <button
                className="button button--primary"
                disabled={selectedMembers.length !== expectedPlayers}
              >
                Thêm entry
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}

export default TournamentPage;
