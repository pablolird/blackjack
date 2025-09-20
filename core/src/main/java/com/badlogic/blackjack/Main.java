    package com.badlogic.blackjack;

    import com.badlogic.gdx.ApplicationListener;
    import com.badlogic.gdx.Gdx;
    import com.badlogic.gdx.graphics.GL20;

    /** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
    public class Main implements ApplicationListener {
        private BlackjackGame game;

        @Override
        public void create() {
            game = new BlackjackGame();
        }

        @Override
        public void resize(int width, int height) {
            // If the window is minimized on a desktop (LWJGL3) platform, width and height are 0, which causes problems.
            // In that case, we don't resize anything, and wait for the window to be a normal size before updating.
            if(width <= 0 || height <= 0) return;

            // Resize your application here. The parameters represent the new window size.
        }

        @Override
        public void render() {
            float delta = Gdx.graphics.getDeltaTime();

            // Game loop
            game.update(delta);

            // Clear screen
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            // Render
            game.render();
        }

        @Override
        public void pause() {
            // Invoked when your application is paused.
        }

        @Override
        public void resume() {
            // Invoked when your application is resumed after pause.
        }

        @Override
        public void dispose() {
            // Destroy application's resources here.
        }
    }
