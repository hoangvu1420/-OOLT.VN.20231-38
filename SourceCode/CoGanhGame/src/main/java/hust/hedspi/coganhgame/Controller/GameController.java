package hust.hedspi.coganhgame.Controller;

import hust.hedspi.coganhgame.ComponentView.PieceComp;
import hust.hedspi.coganhgame.ComponentView.TileComp;
import hust.hedspi.coganhgame.Model.Game.Game;
import hust.hedspi.coganhgame.Model.Game.GameWithBot;
import hust.hedspi.coganhgame.Model.Move.Move;
import hust.hedspi.coganhgame.Model.Move.MoveResult;
import hust.hedspi.coganhgame.Model.Piece;
import hust.hedspi.coganhgame.Model.Player.BotPlayer;
import hust.hedspi.coganhgame.Model.Player.HumanPlayer;
import hust.hedspi.coganhgame.Model.Tile.Tile;
import hust.hedspi.coganhgame.Utilities.AdaptiveUtilities;
import hust.hedspi.coganhgame.Utilities.Constants;
import hust.hedspi.coganhgame.Utilities.ViewUtilities;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class GameController {
    private final Game game;
    @FXML
    public Pane boardPane;
    @FXML
    public Button btnExit;
    @FXML
    public Label currentNameLabel;
    @FXML
    public VBox vbBoard;
    @FXML
    public Label currentLabel;
    @FXML
    public ProgressBar prbTimeLeft;
    @FXML
    public Label lblTotalPiecesRed;
    @FXML
    public Label lblTotalPiecesBlue;
    @FXML
    public Label lblTotalTimeBlue;
    @FXML
    public Label lblTotalTimeRed;
    @FXML
    public VBox vbRed;
    @FXML
    public VBox vbBlue;
    @FXML
    public Label lblBotLevel;
    @FXML
    public HBox hbBotLevel;
    @FXML
    public Label player1NameLabel;
    @FXML
    public Label player2NameLabel;
    @FXML
    public Label botPositionCountLabel;
    @FXML
    public Button btnReset;
    @FXML
    public Button btnOpenRed;
    @FXML
    public Button btnOpenBlue;
    @FXML
    public Button btnPassBlue;
    @FXML
    public Button btnPassRed;
    @FXML
    public HBox hbOpenRed;
    @FXML
    public HBox hbOpenBlue;
    private int botPositionCount = -1;

    private Tile currentTile;
    private Tile draggedTile;

    private final Group tileCompGroup = new Group();
    private final Group pieceCompGroup = new Group();
    private final TileComp[][] viewBoard = new TileComp[Constants.WIDTH][Constants.HEIGHT];
    private final Map<Piece, PieceComp> pieceMap = new HashMap<>();
    private final ChangeListener<Number> timeLeftListener = (observable, oldValue, newValue) -> {
        if (newValue.intValue() <= 0) {
            clearOpenHighlight();
            switchPlayer();
        }
    };
    private final Timeline timeline = new Timeline();
    private ExecutorService executor = Executors.newSingleThreadExecutor(); // this executor is used to run the botMoveTask

    public GameController(String player1Name, String player2Name, int timeLimit) {
        this.game = new Game(player1Name, player2Name, timeLimit);
    }

    public GameController(String player1Name, int timeLimit, int botLevel) {
        this.game = new GameWithBot(player1Name, timeLimit, botLevel);
    }

    public GameController(Game game) {
        this.game = game;
    }

    @FXML
    public void initialize() {
        initViewBoard();
        boardPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE); // this line is used to make the boardPane fit the size of the board
        boardPane.setPrefSize(AdaptiveUtilities.BOARD_WIDTH, AdaptiveUtilities.BOARD_HEIGHT);
        boardPane.getChildren().addAll(tileCompGroup, pieceCompGroup);
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO, new KeyValue(prbTimeLeft.progressProperty(), 1)),
                new KeyFrame(Duration.seconds(game.getTimeLimit()), new KeyValue(prbTimeLeft.progressProperty(), 0))
        );

        if (!(game instanceof GameWithBot)) {
            vbBlue.getChildren().remove(botPositionCountLabel);
            vbBlue.getChildren().remove(hbBotLevel);
            HBox.setMargin(player2NameLabel, new Insets(0, 0, 10, 0));
        } else {
            switch (((BotPlayer) game.getPlayer2()).getBotLevel()) {
                case Constants.BOT_LEVEL_EASY -> {
                    lblBotLevel.setText("Easy");
                    lblBotLevel.setTextFill(Color.web("#5D9C59"));
                }
                case Constants.BOT_LEVEL_MEDIUM -> {
                    lblBotLevel.setText("Medium");
                    lblBotLevel.setTextFill(Color.web("#6C6B30"));
                }
                case Constants.BOT_LEVEL_HARD -> {
                    lblBotLevel.setText("Hard");
                    lblBotLevel.setTextFill(Color.web("#B31312"));
                }
            }
            botPositionCountLabel.setText("Position count: " + (BotPlayer.positionCount));
        }
        vbRed.getChildren().remove(hbOpenRed);
        vbBlue.getChildren().remove(hbOpenBlue);
        if (game.isOpening()) {
            ArrayList<Tile> openTiles = game.checkOpeningTile(game.getOpeningTile(), game.getOpponent().getSide());
            viewBoard[game.getOpeningTile().getRow()][game.getOpeningTile().getCol()].highlight(game.getCurrentPlayer().getSide());
            for (Tile openTile : openTiles) {
                PieceComp openPieceComp = pieceMap.get(openTile.getPiece());
                openPieceComp.highlightOpen();
            }
        }

        ((HumanPlayer) game.getCurrentPlayer()).getTimeLeft().addListener(timeLeftListener);
        game.getCurrentPlayer().playTimer();
        updateCurrentPlayerLabel();
        currentLabel.setText("'s turn");
        player1NameLabel.setText(game.getPlayer1().getName());
        player1NameLabel.setTextFill(ViewUtilities.RED_PIECE_COLOR);
        player2NameLabel.setText(game.getPlayer2().getName());
        player2NameLabel.setTextFill(ViewUtilities.BUE_PIECE_COLOR);
        lblTotalPiecesRed.setText("x " + game.getPlayer1().getTotalPiece());
        lblTotalPiecesBlue.setText("x " + game.getPlayer2().getTotalPiece());
        lblTotalTimeRed.setText("Total time: " + ((double) game.getPlayer1().getTotalTime() / 1000) + "s");
        lblTotalTimeBlue.setText("Total time: " + ((double) game.getPlayer2().getTotalTime() / 1000) + "s");
        runTimer();
    }

    private void initViewBoard() {
        Tile[][] modelBoard = game.getBoard();

        for (int row = 0; row < Constants.HEIGHT; row++) {
            for (int col = 0; col < Constants.WIDTH; col++) {
                TileComp tileComp = new TileComp(row, col);
                viewBoard[row][col] = tileComp;

                // we use numbers from 1-5 to represent the rows of the board
                // and letters from A-E to represent the columns of the board
                if (col == 0 || col == Constants.WIDTH - 1) {
                    Label label = new Label(String.valueOf(Constants.HEIGHT - row));
                    label.setTranslateX(col == 0 ? -(AdaptiveUtilities.PIECE_SIZE * 1.5) : (AdaptiveUtilities.PIECE_SIZE * 1.5));
                    label.setFont(ViewUtilities.COOR_FONT);
                    tileComp.getChildren().add(label);
                }
                if (row == 0 || row == Constants.HEIGHT - 1) {
                    Label label = new Label(String.valueOf((char) (col + 65)));
                    label.setTranslateY(row == 0 ? -(AdaptiveUtilities.PIECE_SIZE * 1.5) : (AdaptiveUtilities.PIECE_SIZE * 1.5 + 2));
                    label.setFont(ViewUtilities.COOR_FONT);
                    tileComp.getChildren().add(label);
                }

                Tile modelTile = modelBoard[row][col];
                if (modelTile.hasPiece()) {
                    PieceComp pieceComp = makePieceComp(modelTile.getPiece().getSide(), row, col);
                    pieceCompGroup.getChildren().add(pieceComp);
                    pieceMap.put(modelTile.getPiece(), pieceComp);
                    if (modelTile.getPiece().getSide() != game.getCurrentPlayer().getSide()) {
                        pieceComp.setDisablePiece();
                    }
                }

                tileCompGroup.getChildren().add(tileComp);
            }
        }
    }

    private PieceComp makePieceComp(boolean side, int row, int col) {
        PieceComp pieceComp = new PieceComp(side, row, col);
        AtomicReference<Double> mouseX = new AtomicReference<>((double) 0);
        AtomicReference<Double> mouseY = new AtomicReference<>((double) 0);

        pieceComp.getEllipse().setOnMousePressed(e -> {
            mouseX.set(e.getSceneX());
            mouseY.set(e.getSceneY());
            int rowPressed = toBoardPos(pieceComp.getLayoutY());
            int colPressed = toBoardPos(pieceComp.getLayoutX());
            Tile currentPressedTile = game.getBoard()[rowPressed][colPressed];
            if (currentTile == null) {
                currentTile = currentPressedTile;
            }
                if (!currentTile.equals(currentPressedTile) && !game.isOpening()) { //-----
                    for (Tile move : currentTile.getAvailableMoves(game.getBoard())) {
                        viewBoard[move.getRow()][move.getCol()].removeHighlight();
                    }
                }

            currentTile = game.getBoard()[rowPressed][colPressed];
                if (!game.isOpening()) { //-----
                    for (Tile move : currentTile.getAvailableMoves(game.getBoard())) {
                        viewBoard[move.getRow()][move.getCol()].highlight(currentTile.getPiece().getSide());
                    }
                }
            // bring the piece to the front
            pieceComp.toFront();
        });

        pieceComp.getEllipse().setOnMouseDragged(e -> {
            pieceComp.relocate(e.getSceneX() - mouseX.get() + pieceComp.getOldX(), e.getSceneY() - mouseY.get() + pieceComp.getOldY());
            int rowDragged = toBoardPos(pieceComp.getLayoutY());
            int colDragged = toBoardPos(pieceComp.getLayoutX());
            if (rowDragged < 0 || rowDragged >= Constants.HEIGHT || colDragged < 0 || colDragged >= Constants.WIDTH) {
                return;
            }
            if (draggedTile == null) {
                draggedTile = game.getBoard()[rowDragged][colDragged];
            } else if (rowDragged == draggedTile.getRow() && colDragged == draggedTile.getCol()) {
                return;
            }
            viewBoard[draggedTile.getRow()][draggedTile.getCol()].unfillHighlighter();
            draggedTile = game.getBoard()[rowDragged][colDragged];
            if (draggedTile.equals(currentTile) || currentTile.getAvailableMoves(game.getBoard()).contains(draggedTile)) {
                viewBoard[draggedTile.getRow()][draggedTile.getCol()].fillHighlighter();
            }
        });

        pieceComp.getEllipse().setOnMouseReleased(e -> {
            // when the piece is released, that means the player has finished moving the piece
            // then we process the move
            if (!game.isOpening()) {
                for (Tile move : currentTile.getAvailableMoves(game.getBoard())) {
                    viewBoard[move.getRow()][move.getCol()].removeHighlight();
                }
            }

            int newRow = toBoardPos(pieceComp.getLayoutY());
            int newCol = toBoardPos(pieceComp.getLayoutX());

            int oldRow = toBoardPos(pieceComp.getOldY());
            int oldCol = toBoardPos(pieceComp.getOldX());

            if (oldRow == newRow && oldCol == newCol) {
                // if the piece is not moved, we abort the move
                pieceComp.abortMove();
                return;
            }
            if (newRow < 0 || newRow >= Constants.HEIGHT || newCol < 0 || newCol >= Constants.WIDTH) {
                // if the piece is moved out of the board, we abort the move
                pieceComp.abortMove();
                return;
            }

            Move move = new Move(game.getBoard()[oldRow][oldCol], game.getBoard()[newRow][newCol]);
            if (game.isOpening() && move.toTile() != game.getOpeningTile()) {
                // if the game is in the opening phase and the move is not to the opening tile, we abort the move
                pieceComp.abortMove();
                return;
            } else {
                game.setOpeningTile(null);
            }
            clearOpenHighlight();

            MoveResult moveResult = game.processMove(move); // process the move

            if (moveResult.isValidMove()) {
                pieceComp.move(newRow, newCol);
                if (moveResult.capturedPieces() != null) {
                    // if the move is a capture move, we flip the side of the captured pieces
                    for (Piece capturedModelPiece : moveResult.capturedPieces()) {
                        PieceComp capturedPieceComp = pieceMap.get(capturedModelPiece);
                        capturedPieceComp.flipSide();
                    }
                    lblTotalPiecesRed.setText("x " + game.getPlayer1().getTotalPiece());
                    lblTotalPiecesBlue.setText("x " + game.getPlayer2().getTotalPiece());
                }
                // call the checkOpeningTile method
                ArrayList<Tile> openTiles = game.checkOpeningTile(move.fromTile(), game.getCurrentPlayer().getSide());
                if (!openTiles.isEmpty()) {
                    if (game.getCurrentPlayer().getSide() == Constants.RED_SIDE) {
                        vbRed.getChildren().add(hbOpenRed);
                    } else {
                        vbBlue.getChildren().add(hbOpenBlue);
                    }
                    viewBoard[move.fromTile().getRow()][move.fromTile().getCol()].highlight(game.getOpponent().getSide());
                    for (Tile openTile : openTiles) {
                        PieceComp openPieceComp = pieceMap.get(openTile.getPiece());
                        openPieceComp.highlightOpen();
                    }
                    for (PieceComp piece : pieceMap.values()) {
                        if (piece.getSide() == game.getCurrentPlayer().getSide()) {
                            piece.setDisablePiece();
                        }
                    }
                } else {
                    switchPlayer();
                }
            } else {
                pieceComp.abortMove();
            }
        });
        return pieceComp;
    }

    private int toBoardPos(double pixel) {
        // this method is used to convert the pixel position on the screen to the position on the board
        return (int) ((int) (pixel + AdaptiveUtilities.TILE_SIZE / 2) / AdaptiveUtilities.TILE_SIZE);
    }

    private void switchPlayer() {
        if (game.getCurrentPlayer() instanceof HumanPlayer) {
            game.getCurrentPlayer().pauseTimer();
            ((HumanPlayer) game.getCurrentPlayer()).getTimeLeft().removeListener(timeLeftListener);
            ((HumanPlayer) game.getCurrentPlayer()).setTimeLeft(game.getTimeLimit() * 1000);
            if (game.getCurrentPlayer().getSide() == Constants.RED_SIDE) {
                lblTotalTimeRed.setText("Total time: " + ((double) game.getCurrentPlayer().getTotalTime() / 1000) + "s");
            } else {
                lblTotalTimeBlue.setText("Total time: " + ((double) game.getCurrentPlayer().getTotalTime() / 1000) + "s");
            }
        }
        if (game.isGameOver()) {
            endGame();
            return;
        }
        game.switchPlayer();
        updateCurrentPlayerLabel();
        if (game.getCurrentPlayer() instanceof HumanPlayer) {
            ((HumanPlayer) game.getCurrentPlayer()).getTimeLeft().addListener(timeLeftListener);
            game.getCurrentPlayer().playTimer();
            for (PieceComp piece : pieceMap.values()) {
                if (piece.getSide() == game.getCurrentPlayer().getSide()) {
                    piece.setEnablePiece();
                } else {
                    piece.setDisablePiece();
                }
            }
            runTimer();
        } else {
            for (PieceComp piece : pieceMap.values()) {
                piece.setDisablePiece();
            }
            botMakeMove();
            timeline.stop();
            prbTimeLeft.setProgress(1);
            prbTimeLeft.setStyle("-fx-accent: #2666CF;");
        }
    }

    private void updateCurrentPlayerLabel() {
        if (currentNameLabel != null && game != null && game.getCurrentPlayer() != null) {
            currentNameLabel.setText(game.getCurrentPlayer().getName());
            currentNameLabel.setTextFill(game.getCurrentPlayer().getSide() == Constants.RED_SIDE ? ViewUtilities.RED_PIECE_COLOR : ViewUtilities.BUE_PIECE_COLOR);
        }
    }

    private void runTimer() {
        timeline.stop();
        prbTimeLeft.setProgress(1);
        if (game.getCurrentPlayer().getSide() == Constants.RED_SIDE) { //-----
            prbTimeLeft.setRotate(180);
            prbTimeLeft.setStyle("-fx-accent: #E21818;");
        } else {
            prbTimeLeft.setRotate(0);
            prbTimeLeft.setStyle("-fx-accent: #2666CF;");
        }
        timeline.playFromStart();
    }

    private void botMakeMove() {
        // Create a new Task
        Task<Move> botMoveTask = new Task<>() {
            @Override
            protected Move call() {
                // Perform the long-running operation (bot deciding its move)
                BotPlayer botPlayer = (BotPlayer) game.getCurrentPlayer();
                botPlayer.playTimer();
                Move botMove = botPlayer.getBestMove((GameWithBot) game);
                botPlayer.pauseTimer();
                return botMove;
            }
        };

        // Set up what to do when the Task is done
        botMoveTask.setOnSucceeded(event -> {
            Move botMove = botMoveTask.getValue();

            PieceComp botPieceComp = pieceMap.get(botMove.fromTile().getPiece());
            MoveResult botMoveResult = game.processMove(botMove);
            clearOpenHighlight();
            botPieceComp.slowMove(botMove.toTile().getRow(), botMove.toTile().getCol());

            PauseTransition pause = new PauseTransition(Duration.seconds(Constants.BOT_MOVE_DELAY));
            pause.setOnFinished(e -> {
                if (botMoveResult.capturedPieces() != null) {
                    // if the move is a capture move, we flip the side of the captured pieces
                    for (Piece capturedModelPiece : botMoveResult.capturedPieces()) {
                        PieceComp capturedPieceComp = pieceMap.get(capturedModelPiece);
                        capturedPieceComp.flipSide();
                    }
                }
                botPositionCount = BotPlayer.positionCount;
                updateBotPositionCountLabel();
                lblTotalTimeBlue.setText("Total time: " + ((double) game.getPlayer2().getTotalTime() / 1000) + "s");
                BotPlayer.positionCount = 0;
                switchPlayer();
            });
            pause.play();
        });

        // get the executor to run the task
        executor.execute(botMoveTask);
    }

    private void updateBotPositionCountLabel() {
        if (botPositionCount != -1) {
            botPositionCountLabel.setText("Position count: " + botPositionCount);
        }
    }

    private void endGame() {
        executor.shutdown(); // shutdown the executor to avoid memory leak
        prbTimeLeft.setProgress(1);
        timeline.stop();
        for (PieceComp piece : pieceMap.values()) {
            piece.setDisablePiece();
        }
        currentLabel.setText(" win");
        if (game.getCurrentPlayer().getSide() == Constants.RED_SIDE) {
            prbTimeLeft.setStyle("-fx-accent: #E21818;");
        } else {
            prbTimeLeft.setStyle("-fx-accent: #2666CF;");
        }
    }

    private void clearOpenHighlight() {
        if (currentTile == null) {
            return;
        }
        viewBoard[currentTile.getRow()][currentTile.getCol()].removeHighlight();
        for (PieceComp pieceComp : pieceMap.values()) { //-----
            pieceComp.removeHighlightOpen();
        }
    }

    @FXML
    public void onBtnResetClick() {
        if (game.getCurrentPlayer() instanceof HumanPlayer) {
            game.getCurrentPlayer().pauseTimer();
            timeline.pause();
        }

        if (ViewUtilities.showConfirm("Reset Confirmation", "Are you sure you want to reset the game?")) {
            game.resetGame();
            executor = Executors.newSingleThreadExecutor(); // recreate the executor
            boardPane.getChildren().clear();
            tileCompGroup.getChildren().clear();
            pieceCompGroup.getChildren().clear();
            pieceMap.clear();
            initialize();
        } else {
            if (game.getCurrentPlayer() instanceof HumanPlayer) {
                game.getCurrentPlayer().playTimer();
                timeline.play();
            }
        }
    }

    @FXML
    public void onBtnOpenClick() {
        vbRed.getChildren().remove(hbOpenRed);
        vbBlue.getChildren().remove(hbOpenBlue);
        game.setOpeningTile(currentTile);
        switchPlayer();
    }

    @FXML
    public void onBtnPassClick() {
        vbRed.getChildren().remove(hbOpenRed);
        vbBlue.getChildren().remove(hbOpenBlue);
        clearOpenHighlight();
        switchPlayer();
    }

    @FXML
    public void onBtnExitClick(ActionEvent actionEvent) {
        if (game.getCurrentPlayer() instanceof HumanPlayer) {
            game.getCurrentPlayer().pauseTimer();
            timeline.pause();
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Confirmation");
        alert.setHeaderText(null);

        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        if (!game.isGameOver()) {
            alert.getButtonTypes().setAll(yesButton, noButton, cancelButton);
            alert.setContentText("The game is not over yet. Do you want to save the game before exit?");
            ButtonType result = alert.showAndWait().orElse(cancelButton);
            if (result == yesButton) {
                game.saveGame();
                // User chose Yes -> hide the current stage
                Node source = (Node) actionEvent.getSource();
                Stage currentStage = (Stage) source.getScene().getWindow();
                currentStage.hide();
            } else if (result == noButton) {
                // User chose No -> hide the current stage
                Node source = (Node) actionEvent.getSource();
                Stage currentStage = (Stage) source.getScene().getWindow();
                currentStage.hide();
            } else {
                // User chose Cancel or closed the dialog -> play the timer again
                if (game.getCurrentPlayer() instanceof HumanPlayer) {
                    game.getCurrentPlayer().playTimer();
                    timeline.play();
                }
            }
        } else {
            alert.getButtonTypes().setAll(yesButton, noButton);
            alert.setContentText("Are you sure you want to exit?");
            ButtonType result = alert.showAndWait().orElse(noButton);
            if (result == yesButton) {
                Node source = (Node) actionEvent.getSource();
                Stage currentStage = (Stage) source.getScene().getWindow();
                currentStage.hide();
            }
        }
    }
}
