import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { getApiErrorMessage } from "../api/errors";
import { getGroups, type GroupSummary } from "../api/groups";
import {
  cancelBadmintonSession,
  checkInBadminton,
  confirmBadmintonSession,
  confirmMatch,
  createBadmintonCourt,
  createBadmintonSession,
  createBadmintonVenue,
  createMatch,
  generateCheckinToken,
  generateAllocation,
  getBadmintonSeasons,
  getBadmintonSessions,
  getBadmintonVenues,
  getLeaderboard,
  getMatches,
  getNews,
  getPairings,
  joinBadmintonSession,
  leaveBadmintonSession,
  noShowBadminton,
  openBadmintonSession,
  startBadmintonSession,
  startMatch,
  submitMatchScore,
  type Allocation,
  type BadmintonSeason,
  type BadmintonSession,
  type Match,
  type News,
  type Pairing,
  type Stat,
  type Venue,
} from "../api/badminton";
import { useAuth } from "../auth/AuthContext";
import WorkspaceTabs from "../components/WorkspaceTabs";

function localDateTime(value: string) {
  const date = new Date(value);
  const pad = (part: number) => String(part).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function BadmintonPage() {
  const { user } = useAuth();
  const [params] = useSearchParams();
  const [groups, setGroups] = useState<GroupSummary[]>([]);
  const [checkinTokens, setCheckinTokens] = useState<Record<number, string>>(
    {},
  );
  const [groupId, setGroupId] = useState(Number(params.get("groupId") ?? 0));
  const [seasons, setSeasons] = useState<BadmintonSeason[]>([]);
  const [venues, setVenues] = useState<Venue[]>([]);
  const [sessions, setSessions] = useState<BadmintonSession[]>([]);
  const [allocations, setAllocations] = useState<Allocation[]>([]);
  const [pairings, setPairings] = useState<Pairing[]>([]);
  const [matches, setMatches] = useState<Match[]>([]);
  const [leaderboard, setLeaderboard] = useState<Stat[]>([]);
  const [news, setNews] = useState<News[]>([]);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [scores, setScores] = useState<Record<number, [string, string]>>({});
  const [title, setTitle] = useState("");
  const [start, setStart] = useState(
    params.get("start") ? localDateTime(params.get("start")!) : "",
  );
  const [end, setEnd] = useState(
    params.get("end") ? localDateTime(params.get("end")!) : "",
  );
  const [deadline, setDeadline] = useState("");
  const [venueId, setVenueId] = useState(0);
  const [courtIds, setCourtIds] = useState<number[]>([]);
  const [capacity, setCapacity] = useState("16");
  const [venueName, setVenueName] = useState("");
  const [venueAddress, setVenueAddress] = useState("");
  const [courtName, setCourtName] = useState("");
  const [organizerToolsOpen, setOrganizerToolsOpen] = useState(false);

  const badmintonGroups = groups.filter((group) => group.type === "BADMINTON");
  const selectedGroup = groups.find((group) => group.id === groupId);
  const selectedVenue = venues.find((venue) => venue.id === venueId);
  const organizer =
    selectedGroup?.role === "OWNER" || selectedGroup?.role === "ORGANIZER";

  async function refresh() {
    if (!groupId) return;
    const [nextSessions, nextSeasons, nextVenues, nextMatches, nextNews] =
      await Promise.all([
        getBadmintonSessions(groupId),
        getBadmintonSeasons(groupId),
        getBadmintonVenues(groupId),
        getMatches(groupId),
        getNews(groupId),
      ]);
    setSessions(nextSessions);
    setSeasons(nextSeasons);
    setVenues(nextVenues);
    setMatches(nextMatches);
    setNews(nextNews);
    if (nextSeasons[0])
      getLeaderboard(groupId, nextSeasons[0].id)
        .then(setLeaderboard)
        .catch(() => setLeaderboard([]));
    const nextVenue =
      nextVenues.find((venue) => venue.id === venueId) ?? nextVenues[0];
    if (!venueId && nextVenue) setVenueId(nextVenue.id);
    if (courtIds.length === 0 && nextVenue)
      setCourtIds(nextVenue.courts.slice(0, 4).map((court) => court.id));
  }

  useEffect(() => {
    getGroups()
      .then((data) => {
        setGroups(data);
        if (!groupId)
          setGroupId(data.find((group) => group.type === "BADMINTON")?.id ?? 0);
      })
      .catch((e) => setError(getApiErrorMessage(e, "Could not load groups.")));
  }, []);
  useEffect(() => {
    refresh().catch((e) =>
      setError(getApiErrorMessage(e, "Could not load badminton data.")),
    );
  }, [groupId]);

  async function act(fn: () => Promise<unknown>, text: string) {
    setError("");
    try {
      const result = await fn();
      if (Array.isArray(result) && result.length > 0 && "players" in result[0])
        setAllocations(result as Allocation[]);
      setMessage(text);
      await refresh();
    } catch (e) {
      setError(getApiErrorMessage(e, "Badminton action failed."));
    }
  }

  async function createVenue(event: React.FormEvent) {
    event.preventDefault();
    try {
      const venue = await createBadmintonVenue(groupId, {
        name: venueName,
        address: venueAddress,
      });
      setVenueName("");
      setVenueAddress("");
      setVenueId(venue.id);
      setCourtIds([]);
      setMessage("Venue created.");
      await refresh();
    } catch (e) {
      setError(getApiErrorMessage(e, "Could not create venue."));
    }
  }

  async function createCourt(event: React.FormEvent) {
    event.preventDefault();
    try {
      const court = await createBadmintonCourt(groupId, venueId, courtName);
      setCourtName("");
      setCourtIds((current) =>
        current.includes(court.id) ? current : [...current, court.id],
      );
      setMessage("Court created.");
      await refresh();
    } catch (e) {
      setError(getApiErrorMessage(e, "Could not create court."));
    }
  }

  async function create(event: React.FormEvent) {
    event.preventDefault();
    try {
      await createBadmintonSession(groupId, {
        title,
        start: new Date(start).toISOString(),
        end: new Date(end).toISOString(),
        registrationDeadline: new Date(deadline).toISOString(),
        capacity: Number(capacity) || 16,
        seasonId: seasons[0]?.id,
        venueId,
        courtIds,
      });
      setMessage("Badminton session created as draft.");
      setTitle("");
      await refresh();
    } catch (e) {
      setError(getApiErrorMessage(e, "Could not create session."));
    }
  }

  async function makeMatch(pairing: Pairing) {
    const playing = sessions.find((session) => session.status === "PLAYING");
    if (!playing) return;
    try {
      await createMatch(playing.id, {
        courtId: pairing.courtId,
        roundNumber: pairing.roundNumber,
        sideAUserIds: pairing.sideA.map((player) => player.userId),
        sideBUserIds: pairing.sideB.map((player) => player.userId),
      });
      setMessage("Match created from pairing suggestion.");
      await refresh();
    } catch (e) {
      setError(getApiErrorMessage(e, "Could not create match."));
    }
  }
  async function makeCheckinToken(sessionId: number) {
    try {
      const result = await generateCheckinToken(sessionId);
      setCheckinTokens((current) => ({
        ...current,
        [sessionId]: `${window.location.origin}/check-in?token=${result.token}`,
      }));
      setMessage("Check-in token generated.");
    } catch (e) {
      setError(getApiErrorMessage(e, "Could not generate check-in token."));
    }
  }

  async function score(match: Match) {
    const [scoreA, scoreB] = scores[match.id] ?? ["", ""];
    await act(
      () => submitMatchScore(match.id, Number(scoreA), Number(scoreB)),
      "Score submitted.",
    );
  }

  return (
    <section className="activity-page activity-page--badminton badminton-operations">
      <div className="page-heading">
        <div>
          <p className="eyebrow">VẬN HÀNH CẦU LÔNG</p>
          <h1>Buổi chơi của nhóm.</h1>
          <p className="intro">
            Từ check-in đến chia sân, ghép cặp và xác nhận kết quả. Thành tích,
            điểm số và bảng xếp hạng được cập nhật từ một lần nhập.
          </p>
        </div>
        <select
          className="group-picker"
          value={groupId}
          onChange={(e) => setGroupId(Number(e.target.value))}
        >
          {badmintonGroups.map((group) => (
            <option key={group.id} value={group.id}>
              {group.name}
            </option>
          ))}
        </select>
      </div>
      {groupId > 0 && <WorkspaceTabs groupId={groupId} type="BADMINTON" />}
      {error && <div className="alert alert-danger">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}
      {!groupId && (
        <div className="page-panel empty-state">
          Hãy tạo hoặc tham gia một nhóm cầu lông trước.
        </div>
      )}
      {groupId && (
        <>
          <div className="content-grid">
            <div>
              <div className="section-title">BUỔI CHƠI</div>
              {sessions.length === 0 && (
                <div className="page-panel empty-state">
                  Chưa có buổi chơi nào. Organizer có thể mở bộ công cụ để bắt đầu.
                </div>
              )}
              {sessions.map((session) => {
                const mine = session.registrations.find(
                  (registration) => registration.userId === user?.id,
                );
                const checked = session.registrations.filter(
                  (registration) => registration.status === "CHECKED_IN",
                );
                return (
                  <div className="page-panel study-card" key={session.id}>
                    <div className="study-card-header">
                      <div>
                        <span className="source-tag source-badminton">
                          {session.status}
                        </span>
                        <h2>
                          <Link to={`/badminton/sessions/${session.id}`}>
                            {session.title}
                          </Link>
                        </h2>
                        <p>
                          {new Date(session.start).toLocaleString()} –{" "}
                          {new Date(session.end).toLocaleTimeString([], {
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                        </p>
                        <p>
                          {session.venueName} ·{" "}
                          {session.courts.map((court) => court.name).join(", ")}
                        </p>
                      </div>
                      <span className="subtle">
                        {
                          session.registrations.filter(
                            (registration) =>
                              registration.status === "REGISTERED" ||
                              registration.status === "CHECKED_IN",
                          ).length
                        }{" "}
                        / {session.capacity}
                      </span>
                    </div>
                    <div className="study-actions">
                      {session.status === "DRAFT" && organizer && (
                        <button
                          className="btn btn-outline-primary"
                          onClick={() =>
                            act(
                              () => openBadmintonSession(session.id),
                              "Session opened.",
                            )
                          }
                        >
                          Open
                        </button>
                      )}
                      {session.status === "OPEN" && organizer && (
                        <button
                          className="btn btn-primary"
                          onClick={() =>
                            act(
                              () => confirmBadmintonSession(session.id),
                              "Session confirmed.",
                            )
                          }
                        >
                          Confirm
                        </button>
                      )}
                      {session.status === "CONFIRMED" && organizer && (
                        <button
                          className="btn btn-primary"
                          onClick={() =>
                            act(
                              () => startBadmintonSession(session.id),
                              "Session started.",
                            )
                          }
                        >
                          Start
                        </button>
                      )}
                      {session.status === "PLAYING" && organizer && (
                        <button
                          className="btn btn-outline-success"
                          onClick={() =>
                            act(
                              () => generateAllocation(session.id),
                              "Courts allocated to checked-in players.",
                            )
                          }
                        >
                          Allocate courts
                        </button>
                      )}
                      {organizer &&
                        (session.status === "CONFIRMED" ||
                          session.status === "PLAYING") && (
                          <button
                            className="btn btn-outline-secondary"
                            onClick={() => makeCheckinToken(session.id)}
                          >
                            Generate check-in token
                          </button>
                        )}
                      {(session.status === "OPEN" ||
                        session.status === "CONFIRMED") &&
                        !mine && (
                          <button
                            className="btn btn-outline-primary"
                            onClick={() =>
                              act(
                                () => joinBadmintonSession(session.id),
                                "Joined; any calendar conflict is a warning.",
                              )
                            }
                          >
                            Join
                          </button>
                        )}
                      {(session.status === "OPEN" ||
                        session.status === "CONFIRMED") &&
                        mine &&
                        mine.status !== "CANCELLED" && (
                          <button
                            className="btn btn-outline-secondary"
                            onClick={() =>
                              act(
                                () => leaveBadmintonSession(session.id),
                                "Registration cancelled; waitlist checked.",
                              )
                            }
                          >
                            Leave
                          </button>
                        )}
                      {(session.status === "OPEN" ||
                        session.status === "CONFIRMED") &&
                        organizer && (
                          <button
                            className="btn btn-outline-danger"
                            onClick={() =>
                              act(
                                () => cancelBadmintonSession(session.id),
                                "Session cancelled.",
                              )
                            }
                          >
                            Cancel
                          </button>
                        )}
                    </div>
                    {checkinTokens[session.id] && (
                      <p className="hint">
                        Share this check-in link:{" "}
                        <a href={checkinTokens[session.id]}>
                          {checkinTokens[session.id]}
                        </a>
                      </p>
                    )}
                    <div className="participant-pills">
                      {session.registrations
                        .filter(
                          (registration) => registration.status !== "CANCELLED",
                        )
                        .map((registration) => (
                          <span key={registration.id}>
                            {registration.displayName} · {registration.status}
                            {registration.conflictWarning
                              ? " · conflict warning"
                              : ""}
                            {organizer &&
                              registration.status === "REGISTERED" && (
                                <>
                                  <button
                                    className="mini-button"
                                    onClick={() =>
                                      act(
                                        () =>
                                          checkInBadminton(
                                            session.id,
                                            registration.userId,
                                          ),
                                        "Player checked in.",
                                      )
                                    }
                                  >
                                    check in
                                  </button>
                                  <button
                                    className="mini-button"
                                    onClick={() =>
                                      act(
                                        () =>
                                          noShowBadminton(
                                            session.id,
                                            registration.userId,
                                          ),
                                        "Player marked no-show.",
                                      )
                                    }
                                  >
                                    no-show
                                  </button>
                                </>
                              )}
                          </span>
                        ))}
                    </div>
                    <p className="hint">
                      Checked in: {checked.length}. Generate allocation only
                      after check-in.
                    </p>
                  </div>
                );
              })}
            </div>
            <aside className="form-column organizer-toolbox">
              <button
                type="button"
                className="button button--secondary organizer-toolbox__toggle"
                aria-expanded={organizerToolsOpen}
                onClick={() => setOrganizerToolsOpen((open) => !open)}
              >
                {organizerToolsOpen ? "Đóng bộ công cụ" : "Mở bộ công cụ organizer"}
              </button>
              {organizerToolsOpen && <div className="organizer-toolbox__body">
              {organizer && (
                <>
                  <form
                    className="page-panel form-stack"
                    onSubmit={createVenue}
                  >
                    <div>
                      <p className="eyebrow">ORGANIZER</p>
                      <h2>Thêm địa điểm</h2>
                    </div>
                    <label>
                      Tên địa điểm
                      <input
                        value={venueName}
                        onChange={(e) => setVenueName(e.target.value)}
                        required
                      />
                    </label>
                    <label>
                      Địa chỉ
                      <input
                        value={venueAddress}
                        onChange={(e) => setVenueAddress(e.target.value)}
                      />
                    </label>
                    <button className="btn btn-outline-primary">
                      Thêm địa điểm
                    </button>
                  </form>
                  {venueId > 0 && (
                    <form
                      className="page-panel form-stack"
                      onSubmit={createCourt}
                    >
                      <h2>Thêm sân</h2>
                      <p className="hint">Địa điểm: {selectedVenue?.name}</p>
                      <label>
                        Tên sân
                        <input
                          value={courtName}
                          onChange={(e) => setCourtName(e.target.value)}
                          required
                        />
                      </label>
                      <button className="btn btn-outline-primary">
                        Thêm sân
                      </button>
                    </form>
                  )}
                </>
              )}
              <form className="page-panel form-stack" onSubmit={create}>
                <div>
                  <p className="eyebrow">ORGANIZER</p>
                  <h2>Tạo buổi chơi</h2>
                </div>
                <label>
                  Tên buổi chơi
                  <input
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    required
                  />
                </label>
                <label>
                  Bắt đầu
                  <input
                    type="datetime-local"
                    value={start}
                    onChange={(e) => setStart(e.target.value)}
                    required
                  />
                </label>
                <label>
                  Kết thúc
                  <input
                    type="datetime-local"
                    value={end}
                    onChange={(e) => setEnd(e.target.value)}
                    required
                  />
                </label>
                <label>
                  Hạn đăng ký
                  <input
                    type="datetime-local"
                    value={deadline}
                    onChange={(e) => setDeadline(e.target.value)}
                    required
                  />
                </label>
                <label>
                  Sức chứa
                  <input
                    type="number"
                    min="1"
                    value={capacity}
                    onChange={(e) => setCapacity(e.target.value)}
                  />
                </label>
                <label>
                  Địa điểm
                  <select
                    value={venueId}
                    onChange={(e) => {
                      const id = Number(e.target.value);
                      setVenueId(id);
                      setCourtIds(
                        venues
                          .find((venue) => venue.id === id)
                          ?.courts.slice(0, 4)
                          .map((court) => court.id) ?? [],
                      );
                    }}
                  >
                    {venues.map((venue) => (
                      <option key={venue.id} value={venue.id}>
                        {venue.name}
                      </option>
                    ))}
                  </select>
                </label>
                <div>
                  <div className="form-label">Sân sử dụng</div>
                  {(selectedVenue?.courts ?? []).map((court) => (
                    <label className="weekday-check" key={court.id}>
                      <input
                        type="checkbox"
                        checked={courtIds.includes(court.id)}
                        onChange={(e) =>
                          setCourtIds(
                            e.target.checked
                              ? [...courtIds, court.id]
                              : courtIds.filter((id) => id !== court.id),
                          )
                        }
                      />
                      {court.name}
                    </label>
                  ))}
                </div>
                <button
                  className="btn btn-primary"
                  disabled={!seasons[0] || !venueId || courtIds.length === 0}
                >
                  Tạo bản nháp
                </button>
              </form>
              </div>}
            </aside>
          </div>
          <div className="content-grid dashboard-grid">
            <div className="page-panel">
              <div className="section-title">
                CHIA SÂN & GHÉP CẶP
              </div>
              {allocations.map((allocation) => (
                <div className="saved-source" key={allocation.id}>
                  <strong>
                    {allocation.courtName}:{" "}
                    {allocation.players
                      .map((player) => player.displayName)
                      .join(", ") || "no checked-in players"}
                  </strong>
                  <span>{allocation.status}</span>
                </div>
              ))}
              {sessions
                .filter((session) => session.status === "PLAYING")
                .map((session) => (
                  <div key={session.id}>
                    <button
                      className="btn btn-outline-primary"
                      onClick={() =>
                        getPairings(session.id)
                          .then(setPairings)
                          .catch((e) =>
                            setError(
                              getApiErrorMessage(e, "Could not load pairings."),
                            ),
                          )
                      }
                    >
                      Suggest balanced pairing
                    </button>
                    {pairings.map((pairing) => (
                      <div
                        className="saved-source"
                        key={`${pairing.courtId}-${pairing.roundNumber}`}
                      >
                        <strong>
                          {pairing.courtName}:{" "}
                          {pairing.sideA
                            .map((player) => player.displayName)
                            .join(" + ")}{" "}
                          vs{" "}
                          {pairing.sideB
                            .map((player) => player.displayName)
                            .join(" + ")}
                        </strong>
                        <span>{pairing.unassigned.length} unassigned</span>
                        {pairing.sideA.length > 0 &&
                          pairing.sideB.length > 0 && (
                            <button
                              className="mini-button"
                              onClick={() => makeMatch(pairing)}
                            >
                              Create match
                            </button>
                          )}
                      </div>
                    ))}
                  </div>
                ))}
            </div>
            <div className="page-panel">
              <div className="section-title">BẢNG XẾP HẠNG</div>
              {leaderboard.length === 0 && (
                <p className="hint">
                  Kết quả sẽ xuất hiện sau khi một trận đấu được xác nhận.
                </p>
              )}
              {leaderboard.map((stat) => (
                <div className="member-row" key={stat.userId}>
                  <strong>{stat.displayName}</strong>
                  <span>
                    {stat.points} pts · {stat.wins}W/{stat.losses}L ·{" "}
                    {stat.recentForm || "—"}
                  </span>
                </div>
              ))}
            </div>
          </div>
          <div className="page-panel">
            <div className="section-title">TRẬN ĐẤU & KẾT QUẢ</div>
            {matches.length === 0 && <p className="hint">Chưa có trận đấu nào.</p>}
            {matches.map((match) => (
              <div className="study-card" key={match.id}>
                <div className="study-card-header">
                  <strong>
                    {match.courtName} · Round {match.roundNumber} ·{" "}
                    {match.status}
                  </strong>
                  <span>
                    {match.scoreA ?? "—"} : {match.scoreB ?? "—"}
                  </span>
                </div>
                <p>
                  {match.sides
                    .map(
                      (side) =>
                        `${side.code}: ${side.participants.map((player) => player.displayName).join(" + ")}`,
                    )
                    .join(" · ")}
                </p>
                <div className="study-actions">
                  {match.status === "SCHEDULED" && organizer && (
                    <button
                      className="btn btn-outline-primary"
                      onClick={() =>
                        act(() => startMatch(match.id), "Match started.")
                      }
                    >
                      Start
                    </button>
                  )}
                  {match.status === "PLAYING" && (
                    <>
                      <input
                        className="score-input"
                        type="number"
                        min="0"
                        placeholder="A"
                        onChange={(e) =>
                          setScores({
                            ...scores,
                            [match.id]: [
                              e.target.value,
                              scores[match.id]?.[1] ?? "",
                            ],
                          })
                        }
                      />
                      <input
                        className="score-input"
                        type="number"
                        min="0"
                        placeholder="B"
                        onChange={(e) =>
                          setScores({
                            ...scores,
                            [match.id]: [
                              scores[match.id]?.[0] ?? "",
                              e.target.value,
                            ],
                          })
                        }
                      />
                      <button
                        className="btn btn-primary"
                        onClick={() => score(match)}
                      >
                        Submit score
                      </button>
                    </>
                  )}
                  {match.status === "RESULT_SUBMITTED" && organizer && (
                    <button
                      className="btn btn-primary"
                      onClick={() =>
                        act(
                          () => confirmMatch(match.id),
                          "Result confirmed; stats and news updated.",
                        )
                      }
                    >
                      Confirm result
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
          <div className="page-panel">
            <div className="section-title">TIN TRONG NHÓM</div>
            {news.length === 0 && (
              <p className="hint">No announcements or results yet.</p>
            )}
            {news.map((item) => (
              <div className="saved-source" key={item.id}>
                <strong>{item.title}</strong>
                <span>{item.content}</span>
              </div>
            ))}
          </div>
        </>
      )}
    </section>
  );
}

export default BadmintonPage;
